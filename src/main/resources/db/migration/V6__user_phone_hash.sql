-- Migration V6: Create User Phone Hash table
-- Creates the user_phone_hash table for mutual contacts

CREATE TABLE IF NOT EXISTS user_phone_hash (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone_hash VARCHAR(128) NOT NULL,
    CONSTRAINT unique_user_phone_hash UNIQUE (user_id, phone_hash)
);

CREATE INDEX IF NOT EXISTS idx_user_phone_hash_user ON user_phone_hash(user_id);
CREATE INDEX IF NOT EXISTS idx_user_phone_hash_hash ON user_phone_hash(phone_hash);

COMMENT ON TABLE user_phone_hash IS 'Hashed phone numbers for mutual contacts discovery';
COMMENT ON COLUMN user_phone_hash.phone_hash IS 'SHA-256 or similar hash of phone number';

