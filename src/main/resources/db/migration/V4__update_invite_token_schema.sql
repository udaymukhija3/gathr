-- Migration V4: Update Invite Token table schema
-- Updates the invite_token table to match the simplified schema requirements

-- Note: The existing invite_tokens table may have more columns
-- This migration ensures the required columns exist

-- Add columns if they don't exist (for compatibility)
ALTER TABLE invite_tokens
ADD COLUMN IF NOT EXISTS token VARCHAR(128),
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

-- Ensure token is unique and not null
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'invite_tokens_token_key') THEN
        ALTER TABLE invite_tokens ADD CONSTRAINT invite_tokens_token_key UNIQUE (token);
    END IF;
END $$;

-- Update existing tokens to have expires_at if null
UPDATE invite_tokens SET expires_at = created_at + INTERVAL '48 hours' WHERE expires_at IS NULL;

-- Create indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_invite_token_activity ON invite_tokens(activity_id);
CREATE INDEX IF NOT EXISTS idx_invite_token_token ON invite_tokens(token);
CREATE INDEX IF NOT EXISTS idx_invite_token_expires_at ON invite_tokens(expires_at);

COMMENT ON TABLE invite_tokens IS 'Invite tokens for invite-only activities';
COMMENT ON COLUMN invite_tokens.token IS 'Unique token string for sharing';
COMMENT ON COLUMN invite_tokens.expires_at IS 'When the token expires';

