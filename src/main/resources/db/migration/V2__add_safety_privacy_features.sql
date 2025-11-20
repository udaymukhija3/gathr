-- Migration V2: Add Safety and Privacy Features
-- Adds: is_banned to users, is_invite_only to activities,
-- new tables: blocks, reports, invite_tokens, audit_logs
-- timestamp tracking for participations, is_deleted for messages

-- =====================================================
-- 1. ALTER EXISTING TABLES
-- =====================================================

-- Add is_banned to users table
ALTER TABLE users
ADD COLUMN is_banned BOOLEAN NOT NULL DEFAULT false;

-- Add missing fields to activities table
ALTER TABLE activities
ADD COLUMN is_invite_only BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Add timestamp tracking and +1 feature to participations table
ALTER TABLE participations
ADD COLUMN joined_ts TIMESTAMP DEFAULT NOW(),
ADD COLUMN left_ts TIMESTAMP,
ADD COLUMN brings_plus_one BOOLEAN NOT NULL DEFAULT false;

-- Update participations status enum to include LEFT
-- Note: This depends on your database - PostgreSQL example:
-- ALTER TYPE participation_status ADD VALUE IF NOT EXISTS 'LEFT';

-- Add soft delete to messages table
ALTER TABLE messages
ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_is_banned ON users(is_banned);
CREATE INDEX IF NOT EXISTS idx_activities_is_invite_only ON activities(is_invite_only);
CREATE INDEX IF NOT EXISTS idx_activities_created_at ON activities(created_at);
CREATE INDEX IF NOT EXISTS idx_messages_is_deleted ON messages(is_deleted);
CREATE INDEX IF NOT EXISTS idx_participations_joined_ts ON participations(joined_ts);

-- =====================================================
-- 2. CREATE NEW TABLES
-- =====================================================

-- Blocks table
CREATE TABLE IF NOT EXISTS blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reason VARCHAR(500),
    CONSTRAINT unique_block UNIQUE (blocker_id, blocked_id),
    CONSTRAINT no_self_block CHECK (blocker_id != blocked_id)
);

CREATE INDEX idx_blocks_blocker ON blocks(blocker_id);
CREATE INDEX idx_blocks_blocked ON blocks(blocked_id);
CREATE INDEX idx_blocks_created_at ON blocks(created_at);

-- Reports table
CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_id BIGINT REFERENCES messages(id) ON DELETE SET NULL,
    activity_id BIGINT REFERENCES activities(id) ON DELETE SET NULL,
    reason VARCHAR(50) NOT NULL CHECK (reason IN (
        'HARASSMENT', 'INAPPROPRIATE_CONTENT', 'SPAM',
        'FAKE_PROFILE', 'SAFETY_CONCERN', 'NO_SHOW', 'OTHER'
    )),
    details TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'UNDER_REVIEW', 'RESOLVED_ACTION_TAKEN',
        'RESOLVED_NO_ACTION', 'DISMISSED'
    )),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    admin_notes TEXT
);

CREATE INDEX idx_reports_target_user ON reports(target_user_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_created_at ON reports(created_at);
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_activity ON reports(activity_id);

-- Invite Tokens table
CREATE TABLE IF NOT EXISTS invite_tokens (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    token VARCHAR(100) UNIQUE NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    use_count INTEGER NOT NULL DEFAULT 0,
    max_uses INTEGER,
    revoked BOOLEAN NOT NULL DEFAULT false,
    invited_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    note VARCHAR(500)
);

CREATE INDEX idx_invite_tokens_token ON invite_tokens(token);
CREATE INDEX idx_invite_tokens_activity ON invite_tokens(activity_id);
CREATE INDEX idx_invite_tokens_expires_at ON invite_tokens(expires_at);
CREATE INDEX idx_invite_tokens_created_by ON invite_tokens(created_by);

-- Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL CHECK (action IN (
        'USER_CREATED', 'USER_BANNED', 'USER_UNBANNED', 'USER_VERIFIED',
        'ACTIVITY_CREATED', 'ACTIVITY_DELETED', 'ACTIVITY_MODIFIED',
        'REPORT_CREATED', 'REPORT_REVIEWED', 'REPORT_RESOLVED',
        'BLOCK_CREATED', 'BLOCK_REMOVED',
        'MESSAGE_DELETED', 'MESSAGE_FLAGGED',
        'INVITE_CREATED', 'INVITE_REVOKED',
        'AUTO_BAN_TRIGGERED', 'PASSWORD_RESET', 'DATA_EXPORTED', 'DATA_DELETED'
    )),
    entity VARCHAR(50) NOT NULL CHECK (entity IN (
        'USER', 'ACTIVITY', 'MESSAGE', 'REPORT', 'BLOCK',
        'INVITE_TOKEN', 'PARTICIPATION', 'HUB'
    )),
    entity_id BIGINT NOT NULL,
    details TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

-- =====================================================
-- 3. DATA BACKFILL (for existing records)
-- =====================================================

-- Set created_at for existing activities (approximate from ID or use NOW)
UPDATE activities
SET created_at = NOW()
WHERE created_at IS NULL;

-- Set joined_ts for existing participations
UPDATE participations
SET joined_ts = NOW()
WHERE joined_ts IS NULL;

-- =====================================================
-- 4. COMMENTS
-- =====================================================

COMMENT ON TABLE blocks IS 'User block relationships for safety';
COMMENT ON TABLE reports IS 'User reports for moderation';
COMMENT ON TABLE invite_tokens IS 'Invite tokens for invite-only activities';
COMMENT ON TABLE audit_logs IS 'Audit trail for admin/system actions';

COMMENT ON COLUMN users.is_banned IS 'Whether user is banned from platform';
COMMENT ON COLUMN activities.is_invite_only IS 'Whether activity requires invitation';
COMMENT ON COLUMN participations.brings_plus_one IS 'Whether user is bringing a +1 guest';
COMMENT ON COLUMN messages.is_deleted IS 'Soft delete flag for ephemeral messages';
