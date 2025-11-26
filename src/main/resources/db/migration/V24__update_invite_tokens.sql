-- Update invite_tokens table
ALTER TABLE invite_tokens
  ADD COLUMN IF NOT EXISTS issuer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS max_uses INT DEFAULT 1,
  ADD COLUMN IF NOT EXISTS uses INT DEFAULT 0;

-- Add check constraint
ALTER TABLE invite_tokens DROP CONSTRAINT IF EXISTS chk_uses_max;
ALTER TABLE invite_tokens ADD CONSTRAINT chk_uses_max CHECK (uses <= max_uses);
