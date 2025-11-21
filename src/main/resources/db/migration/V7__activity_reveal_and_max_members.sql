-- Migration V7: Add reveal_identities and max_members to activities
-- Adds identity reveal and max members features to activities

ALTER TABLE activities
ADD COLUMN IF NOT EXISTS max_members INTEGER NOT NULL DEFAULT 4,
ADD COLUMN IF NOT EXISTS reveal_identities BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_activities_max_members ON activities(max_members);
CREATE INDEX IF NOT EXISTS idx_activities_reveal_identities ON activities(reveal_identities);

COMMENT ON COLUMN activities.max_members IS 'Maximum number of participants allowed';
COMMENT ON COLUMN activities.reveal_identities IS 'Whether participant identities are revealed';

