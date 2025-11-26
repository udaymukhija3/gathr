-- Update contact_hashes table (renamed from user_phone_hash)
ALTER TABLE contact_hashes
  ADD COLUMN IF NOT EXISTS hash_algo VARCHAR(50) DEFAULT 'sha256',
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_contact_hashes_user ON contact_hashes(user_id);
CREATE INDEX IF NOT EXISTS idx_contact_hashes_phonehash ON contact_hashes(phone_hash);
