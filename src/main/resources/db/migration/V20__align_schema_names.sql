-- Rename tables to plural/standard names as requested
ALTER TABLE IF EXISTS report RENAME TO reports;
ALTER TABLE IF EXISTS user_phone_hash RENAME TO contact_hashes;
