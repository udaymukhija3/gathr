# Database Backup Strategy

## Overview

This document outlines the backup and disaster recovery strategy for Gathr's PostgreSQL database. A comprehensive backup strategy is critical for data protection, disaster recovery, and regulatory compliance.

## Backup Types

### 1. Full Database Backups

**Frequency**: Daily at 2:00 AM UTC

**Method**: `pg_dump` with custom format

**Retention**: 30 days

**Storage**:
- Primary: AWS S3 / Google Cloud Storage
- Secondary: Different region for disaster recovery

**Command**:
```bash
pg_dump -h $DB_HOST -U $DB_USER -d gathr \
  --format=custom \
  --compress=9 \
  --file=gathr_full_$(date +%Y%m%d_%H%M%S).dump
```

### 2. Incremental Backups (WAL Archiving)

**Frequency**: Continuous (every 16MB of WAL data)

**Method**: PostgreSQL Write-Ahead Logging (WAL)

**Retention**: 7 days

**Purpose**: Point-in-time recovery (PITR)

**Configuration** (`postgresql.conf`):
```conf
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://gathr-backups/wal/%f'
archive_timeout = 3600  # Force archive every hour
```

### 3. Snapshot Backups

**Frequency**: Weekly (Sundays at 1:00 AM UTC)

**Method**: Filesystem/Volume snapshots (if using managed PostgreSQL)

**Retention**: 4 weeks

**Use Case**: Quick restore for infrastructure failures

## Backup Schedule

| Backup Type | Frequency | Retention | Storage |
|-------------|-----------|-----------|---------|
| Full Dump | Daily 2 AM | 30 days | S3 Standard |
| WAL Archive | Continuous | 7 days | S3 Standard-IA |
| Snapshot | Weekly Sun 1 AM | 4 weeks | EBS/GCS Snapshots |
| Monthly Archive | 1st of month | 1 year | S3 Glacier |

## Implementation

### Automated Backup Script

Create `/opt/gathr/backup.sh`:

```bash
#!/bin/bash
set -e

# Configuration
BACKUP_DIR="/var/backups/postgres"
S3_BUCKET="s3://gathr-backups"
DB_NAME="gathr"
DB_HOST="${DB_HOST:-localhost}"
DB_USER="${DB_USER:-postgres}"
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.dump"

# Create backup directory if not exists
mkdir -p $BACKUP_DIR

echo "[$(date)] Starting backup of database: $DB_NAME"

# Perform backup
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME \
  --format=custom \
  --compress=9 \
  --file=$BACKUP_FILE

# Check if backup was successful
if [ $? -eq 0 ]; then
    echo "[$(date)] Backup completed: $BACKUP_FILE"

    # Get file size
    SIZE=$(du -h $BACKUP_FILE | cut -f1)
    echo "[$(date)] Backup size: $SIZE"

    # Upload to S3
    echo "[$(date)] Uploading to S3..."
    aws s3 cp $BACKUP_FILE $S3_BUCKET/daily/ \
        --storage-class STANDARD \
        --metadata "db=$DB_NAME,timestamp=$TIMESTAMP"

    if [ $? -eq 0 ]; then
        echo "[$(date)] Upload successful"

        # Remove local backup after successful upload
        rm -f $BACKUP_FILE
        echo "[$(date)] Local backup removed"
    else
        echo "[$(date)] ERROR: Upload failed, keeping local backup"
        exit 1
    fi
else
    echo "[$(date)] ERROR: Backup failed"
    exit 1
fi

# Clean up old backups from S3
echo "[$(date)] Cleaning up backups older than $RETENTION_DAYS days..."
aws s3 ls $S3_BUCKET/daily/ | while read -r line; do
    BACKUP_DATE=$(echo $line | awk '{print $1}')
    BACKUP_FILE=$(echo $line | awk '{print $4}')

    if [[ $(date -d "$BACKUP_DATE" +%s) -lt $(date -d "$RETENTION_DAYS days ago" +%s) ]]; then
        echo "[$(date)] Deleting old backup: $BACKUP_FILE"
        aws s3 rm $S3_BUCKET/daily/$BACKUP_FILE
    fi
done

echo "[$(date)] Backup process completed successfully"
```

### Set Permissions

```bash
chmod +x /opt/gathr/backup.sh
chown postgres:postgres /opt/gathr/backup.sh
```

### Schedule with Cron

Add to `/etc/cron.d/gathr-backup`:

```cron
# Daily backup at 2 AM UTC
0 2 * * * postgres /opt/gathr/backup.sh >> /var/log/gathr-backup.log 2>&1

# Weekly snapshot (if using managed PostgreSQL)
0 1 * * 0 root /opt/gathr/snapshot.sh >> /var/log/gathr-snapshot.log 2>&1

# Monthly archive (move to Glacier)
0 3 1 * * postgres /opt/gathr/archive.sh >> /var/log/gathr-archive.log 2>&1
```

### Systemd Timer (Alternative to Cron)

Create `/etc/systemd/system/gathr-backup.service`:

```ini
[Unit]
Description=Gathr Database Backup
After=postgresql.service

[Service]
Type=oneshot
User=postgres
ExecStart=/opt/gathr/backup.sh
StandardOutput=journal
StandardError=journal
```

Create `/etc/systemd/system/gathr-backup.timer`:

```ini
[Unit]
Description=Daily Gathr Database Backup Timer
Requires=gathr-backup.service

[Timer]
OnCalendar=*-*-* 02:00:00
Persistent=true

[Install]
WantedBy=timers.target
```

Enable:
```bash
systemctl daemon-reload
systemctl enable --now gathr-backup.timer
systemctl list-timers | grep gathr
```

## WAL Archiving Setup

### PostgreSQL Configuration

Add to `postgresql.conf`:

```conf
# WAL Settings
wal_level = replica
archive_mode = on
archive_command = '/opt/gathr/archive_wal.sh %p %f'
archive_timeout = 3600  # Force archive every hour
max_wal_senders = 3
wal_keep_size = 1GB

# Checkpoint tuning for better write performance
checkpoint_timeout = 15min
max_wal_size = 2GB
min_wal_size = 80MB
checkpoint_completion_target = 0.9
```

### WAL Archive Script

Create `/opt/gathr/archive_wal.sh`:

```bash
#!/bin/bash
# %p = path to file to archive
# %f = file name only
WAL_PATH=$1
WAL_FILE=$2
S3_BUCKET="s3://gathr-backups/wal"

# Upload WAL file to S3
aws s3 cp $WAL_PATH $S3_BUCKET/$WAL_FILE

# Return 0 for success (PostgreSQL expects this)
exit $?
```

Set permissions:
```bash
chmod +x /opt/gathr/archive_wal.sh
chown postgres:postgres /opt/gathr/archive_wal.sh
```

## Restore Procedures

### Full Database Restore

#### 1. Restore from Daily Backup

```bash
# Download backup from S3
aws s3 cp s3://gathr-backups/daily/gathr_20250121_020000.dump /tmp/

# Stop application (prevent new connections)
systemctl stop gathr-backend

# Drop existing database (CAREFUL!)
psql -U postgres -c "DROP DATABASE IF EXISTS gathr;"

# Create fresh database
psql -U postgres -c "CREATE DATABASE gathr OWNER gathr_user;"

# Restore from backup
pg_restore -h localhost -U postgres -d gathr \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  --verbose \
  /tmp/gathr_20250121_020000.dump

# Restart application
systemctl start gathr-backend

# Verify
psql -U postgres -d gathr -c "SELECT COUNT(*) FROM users;"
```

#### 2. Point-in-Time Recovery (PITR)

For recovering to a specific timestamp:

```bash
# Stop PostgreSQL
systemctl stop postgresql

# Backup current data directory
mv /var/lib/postgresql/15/main /var/lib/postgresql/15/main.old

# Restore base backup
pg_basebackup -h localhost -U replicator -D /var/lib/postgresql/15/main -P

# Create recovery configuration
cat > /var/lib/postgresql/15/main/recovery.conf <<EOF
restore_command = 'aws s3 cp s3://gathr-backups/wal/%f %p'
recovery_target_time = '2025-01-21 14:30:00 UTC'
recovery_target_action = 'promote'
EOF

# Set permissions
chown -R postgres:postgres /var/lib/postgresql/15/main

# Start PostgreSQL (will enter recovery mode)
systemctl start postgresql

# Monitor recovery
tail -f /var/log/postgresql/postgresql-15-main.log

# Verify recovered state
psql -U postgres -d gathr -c "SELECT now(), version();"
```

### Automated Restore Testing

Create `/opt/gathr/test_restore.sh`:

```bash
#!/bin/bash
set -e

BACKUP_FILE=$1
TEST_DB="gathr_restore_test"

echo "Testing restore of: $BACKUP_FILE"

# Create test database
psql -U postgres -c "DROP DATABASE IF EXISTS $TEST_DB;"
psql -U postgres -c "CREATE DATABASE $TEST_DB;"

# Restore
pg_restore -U postgres -d $TEST_DB --verbose $BACKUP_FILE

# Run verification queries
USERS_COUNT=$(psql -U postgres -d $TEST_DB -t -c "SELECT COUNT(*) FROM users;")
ACTIVITIES_COUNT=$(psql -U postgres -d $TEST_DB -t -c "SELECT COUNT(*) FROM activities;")

echo "Restore test results:"
echo "  Users: $USERS_COUNT"
echo "  Activities: $ACTIVITIES_COUNT"

# Drop test database
psql -U postgres -c "DROP DATABASE $TEST_DB;"

echo "Restore test completed successfully"
```

## Monitoring and Alerts

### Backup Verification Script

Create `/opt/gathr/verify_backups.sh`:

```bash
#!/bin/bash

S3_BUCKET="s3://gathr-backups"
ALERT_EMAIL="ops@gathr.com"
SLACK_WEBHOOK="${SLACK_WEBHOOK_URL}"

# Check if today's backup exists
TODAY=$(date +%Y%m%d)
BACKUP_COUNT=$(aws s3 ls $S3_BUCKET/daily/ | grep $TODAY | wc -l)

if [ $BACKUP_COUNT -eq 0 ]; then
    MESSAGE="❌ ALERT: No backup found for $TODAY"
    echo $MESSAGE

    # Send Slack notification
    curl -X POST $SLACK_WEBHOOK \
        -H 'Content-Type: application/json' \
        -d "{\"text\":\"$MESSAGE\"}"

    # Send email alert
    echo $MESSAGE | mail -s "Gathr Backup Alert" $ALERT_EMAIL

    exit 1
else
    echo "✅ Backup verified for $TODAY"
fi
```

### CloudWatch Monitoring (AWS)

```bash
# Log backup metrics to CloudWatch
aws cloudwatch put-metric-data \
    --namespace "Gathr/Backups" \
    --metric-name BackupSize \
    --value $BACKUP_SIZE_BYTES \
    --unit Bytes

aws cloudwatch put-metric-data \
    --namespace "Gathr/Backups" \
    --metric-name BackupDuration \
    --value $DURATION_SECONDS \
    --unit Seconds
```

## Disaster Recovery Plan

### Recovery Time Objective (RTO)

**Target**: 4 hours

**Scenario**: Complete database loss

**Steps**:
1. Provision new database instance (30 min)
2. Download latest backup from S3 (15 min)
3. Restore database (2 hours for 100GB)
4. Update application configuration (15 min)
5. Verification and testing (1 hour)

### Recovery Point Objective (RPO)

**Target**: 1 hour

**Achieved through**:
- Continuous WAL archiving (max 1-hour data loss)
- Daily full backups as fallback

### Disaster Scenarios

#### Scenario 1: Accidental Table Drop

**Solution**: Point-in-time recovery

```bash
# Restore to 5 minutes before incident
restore_command = '...'
recovery_target_time = '2025-01-21 14:25:00 UTC'
```

#### Scenario 2: Data Corruption

**Solution**: Restore from daily backup

```bash
pg_restore -d gathr gathr_20250121_020000.dump
```

#### Scenario 3: Region Failure

**Solution**: Restore in different region

```bash
# Cross-region replication ensures backup availability
aws s3 sync s3://gathr-backups s3://gathr-backups-eu --source-region us-east-1 --region eu-west-1
```

## Security

### Encryption at Rest

```bash
# Encrypt backups before upload
openssl enc -aes-256-cbc -salt -in gathr.dump -out gathr.dump.enc -k $ENCRYPTION_KEY
aws s3 cp gathr.dump.enc s3://gathr-backups/daily/
```

### Encryption in Transit

```bash
# S3 uploads use SSL by default
# Ensure server-side encryption
aws s3 cp backup.dump s3://gathr-backups/ --sse AES256
```

### Access Control

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::123456789:role/gathr-backup-role"
      },
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::gathr-backups",
        "arn:aws:s3:::gathr-backups/*"
      ]
    }
  ]
}
```

## Testing Backups

### Monthly Restore Test

**Schedule**: First Monday of every month

**Procedure**:
1. Download latest backup
2. Restore to test environment
3. Run data integrity checks
4. Verify application connectivity
5. Document results

**Checklist**:
- [ ] Backup downloads successfully
- [ ] Restore completes without errors
- [ ] All tables present
- [ ] Row counts match expectations
- [ ] Application can connect
- [ ] Queries execute correctly

## Cost Optimization

### S3 Storage Classes

| Backup Type | Storage Class | Cost/GB/Month |
|-------------|---------------|---------------|
| Daily (30 days) | S3 Standard | $0.023 |
| WAL (7 days) | S3 Standard-IA | $0.0125 |
| Monthly (1 year) | S3 Glacier | $0.004 |

### Estimated Costs (for 100GB database)

```
Daily backups: 100GB × 30 days × $0.023 = $69/month
WAL archives: 50GB × 7 days × $0.0125 = $4.38/month
Monthly archives: 100GB × 12 months × $0.004 = $4.80/month
Total: ~$78/month
```

## Compliance

### GDPR Considerations

- Backups contain personal data
- Apply same retention policies as production
- Implement right to be forgotten in backups
- Document backup locations and access logs

### Audit Trail

Log all backup and restore operations:

```bash
echo "$(date) - Backup created: $BACKUP_FILE by $(whoami)" >> /var/log/gathr-backup-audit.log
```

## References

- [PostgreSQL Backup & Restore](https://www.postgresql.org/docs/current/backup.html)
- [WAL Archiving](https://www.postgresql.org/docs/current/continuous-archiving.html)
- [pg_dump Documentation](https://www.postgresql.org/docs/current/app-pgdump.html)
- [AWS RDS Backup](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_CommonTasks.BackupRestore.html)
