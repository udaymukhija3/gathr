-- Migration V8: Add created_at to messages if missing
-- Ensures messages table has created_at for expiry logic

ALTER TABLE messages
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);

COMMENT ON COLUMN messages.created_at IS 'When the message was created (for expiry logic)';

