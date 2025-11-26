-- Update messages table
ALTER TABLE messages
  ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP NULL,
  ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

-- Ensure sender_id and activity_id exist (they should from V8/creation)
-- Add indexes if missing
CREATE INDEX IF NOT EXISTS idx_messages_activity_id ON messages(activity_id);
