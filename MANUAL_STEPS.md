# Manual Steps for Backend Implementation

This document outlines the manual steps required after the code generation to complete the backend implementation.

## 1. Database Setup

### Run Migrations
The Flyway migrations will run automatically on application startup. Ensure your database is accessible:

```bash
# Check database connection
psql -h localhost -U postgres -d gathr -c "SELECT version();"
```

### Verify Migrations
Check that all migrations have been applied:

```bash
# Check Flyway migration history
psql -h localhost -U postgres -d gathr -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

Expected migrations:
- V2__add_safety_privacy_features.sql (existing)
- V3__update_report_schema.sql
- V4__update_invite_token_schema.sql
- V5__events.sql
- V6__user_phone_hash.sql
- V7__activity_reveal_and_max_members.sql
- V8__message_created_at.sql

## 2. Environment Configuration

### Set Slack Webhook URL
Add to your `.env` file or environment:

```bash
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

To create a Slack webhook:
1. Go to https://api.slack.com/apps
2. Create a new app or select existing
3. Go to "Incoming Webhooks"
4. Activate webhooks and create a new webhook
5. Copy the webhook URL

### Verify Application Properties
Ensure `application.properties` has:
- Flyway enabled: `spring.flyway.enabled=true`
- Slack webhook config: `slack.webhook.url=${SLACK_WEBHOOK_URL:}`

## 3. Build and Test

### Build the Project
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Start the Application
```bash
mvn spring-boot:run
```

Or with Docker:
```bash
docker-compose up --build
```

## 4. Verify Endpoints

### Test Reports Endpoint
```bash
# Get JWT token first
TOKEN=$(curl -X POST http://localhost:8080/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone":"1234567890","otp":"123456"}' | jq -r '.token')

# Create a report
curl -X POST http://localhost:8080/reports \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUserId": 2,
    "activityId": 1,
    "reason": "Test report"
  }'
```

### Test Invite Token Generation
```bash
# Generate invite token
curl -X POST http://localhost:8080/activities/1/invite-token \
  -H "Authorization: Bearer $TOKEN"
```

### Test Invite-Only Join
```bash
# Join with token
curl -X POST "http://localhost:8080/activities/1/join?status=INTERESTED&inviteToken=TOKEN_HERE" \
  -H "Authorization: Bearer $TOKEN"
```

### Test Contacts Upload
```bash
curl -X POST http://localhost:8080/contacts/upload \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hashes": ["hash1", "hash2", "hash3"]
  }'
```

### Test OTP Rate Limiting
```bash
# Make 4 requests quickly (should fail on 4th)
for i in {1..4}; do
  curl -X POST http://localhost:8080/auth/otp/start \
    -H "Content-Type: application/json" \
    -d '{"phone":"1234567890"}'
  echo ""
done
```

## 5. Verify Scheduled Jobs

### Check Message Expiry Job
The message expiry job runs every 15 minutes. To verify:

1. Create an activity with end time in the past
2. Create messages for that activity
3. Wait for the scheduled job to run (or trigger manually)
4. Verify messages are deleted

### Check Logs
```bash
# View application logs
tail -f logs/application.log

# Or if using Docker
docker logs gathr-backend -f
```

## 6. Database Verification

### Verify Tables Created
```sql
-- Check all tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Expected tables:
-- report
-- invite_token (or invite_tokens)
-- events
-- user_phone_hash
-- activities (with new columns)
-- messages (with created_at)
```

### Verify Indexes
```sql
-- Check indexes on report table
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'report';

-- Check indexes on events table
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'events';
```

## 7. Integration Testing

### Test Complete Flow
1. **Create Activity**
   ```bash
   curl -X POST http://localhost:8080/activities \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "title": "Test Activity",
       "hubId": 1,
       "category": "SPORTS",
       "startTime": "2024-12-31T18:00:00",
       "endTime": "2024-12-31T20:00:00",
       "isInviteOnly": true,
       "maxMembers": 4
     }'
   ```

2. **Generate Invite Token**
   ```bash
   curl -X POST http://localhost:8080/activities/1/invite-token \
     -H "Authorization: Bearer $TOKEN"
   ```

3. **Join Activity (with token)**
   ```bash
   curl -X POST "http://localhost:8080/activities/1/join?status=CONFIRMED&inviteToken=TOKEN" \
     -H "Authorization: Bearer $TOKEN"
   ```

4. **Send Message**
   ```bash
   curl -X POST http://localhost:8080/activities/1/messages \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"text": "Hello!"}'
   ```

5. **Create Report**
   ```bash
   curl -X POST http://localhost:8080/reports \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "targetUserId": 2,
       "activityId": 1,
       "reason": "Inappropriate behavior"
     }'
   ```

## 8. Performance Considerations

### Database Indexes
All necessary indexes have been created. Monitor query performance:

```sql
-- Check slow queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

### Rate Limiting
OTP rate limiting uses in-memory storage. For production:
- Consider using Redis for distributed rate limiting
- Configure appropriate limits per environment

### Message Expiry
The scheduled job runs every 15 minutes. Adjust frequency if needed:
- Edit `@Scheduled(fixedRate = 900000)` in `MessageExpiryJob.java`
- 900000 = 15 minutes in milliseconds

## 9. Production Checklist

- [ ] Set strong `JWT_SECRET` (min 256 bits)
- [ ] Configure `SLACK_WEBHOOK_URL` for production
- [ ] Enable SSL/TLS for database connections
- [ ] Set up database backups
- [ ] Configure logging levels for production
- [ ] Set up monitoring and alerts
- [ ] Review and adjust rate limits
- [ ] Test all endpoints with production-like data
- [ ] Verify Slack webhook notifications
- [ ] Test message expiry job
- [ ] Verify event logging is working
- [ ] Check mutuals calculation performance

## 10. Troubleshooting

### Migration Issues
If migrations fail:
```bash
# Check Flyway status
mvn flyway:info

# Repair if needed
mvn flyway:repair
```

### Slack Webhook Not Working
- Verify `SLACK_WEBHOOK_URL` is set correctly
- Check application logs for webhook errors
- Test webhook URL manually with curl
- Verify Slack app permissions

### Rate Limiting Not Working
- Check in-memory storage (restarts reset limits)
- Consider Redis for persistent rate limiting
- Verify phone number format consistency

### Message Expiry Not Working
- Verify `@EnableScheduling` is present in `GathrApplication.java`
- Check scheduled job logs
- Verify `created_at` column exists on messages table
- Test with manual query: `SELECT * FROM messages WHERE ...`

## 11. Next Steps

1. **Add Unit Tests**: Expand test coverage for all new services
2. **Add Integration Tests**: Test complete flows end-to-end
3. **Performance Testing**: Load test with realistic data volumes
4. **Security Review**: Review authentication and authorization
5. **Documentation**: Update API documentation (Swagger/OpenAPI)
6. **Monitoring**: Set up application monitoring (Prometheus, Grafana)
7. **Error Handling**: Review and improve error messages
8. **Validation**: Add more input validation where needed

## Support

For issues or questions:
1. Check application logs
2. Verify database state
3. Test endpoints individually
4. Review migration history
5. Check environment variables

