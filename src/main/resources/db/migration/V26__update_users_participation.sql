-- Update users table
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS trust_score SMALLINT DEFAULT 100,
  ADD COLUMN IF NOT EXISTS reveal_until_participants INT DEFAULT 3,
  ADD COLUMN IF NOT EXISTS contacts_opt_in BOOLEAN DEFAULT FALSE;

-- Update participation table
ALTER TABLE participation
  ADD COLUMN IF NOT EXISTS rsvp_status VARCHAR(20) DEFAULT 'INTERESTED',
  ADD COLUMN IF NOT EXISTS attended_at TIMESTAMP NULL;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_activities_start_time ON activities(start_time);
CREATE INDEX IF NOT EXISTS idx_participation_activity_user ON participation(activity_id, user_id);
CREATE INDEX IF NOT EXISTS idx_activities_start_hub ON activities (hub_id, start_time);
CREATE INDEX IF NOT EXISTS idx_participation_activity_status ON participation (activity_id, rsvp_status);
