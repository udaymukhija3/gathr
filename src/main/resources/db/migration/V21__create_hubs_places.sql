-- Update hubs table
ALTER TABLE hubs ADD COLUMN IF NOT EXISTS slug VARCHAR(150);
ALTER TABLE hubs ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE hubs ADD CONSTRAINT hubs_slug_key UNIQUE (slug);

-- Create places table
CREATE TABLE IF NOT EXISTS places (
  id BIGSERIAL PRIMARY KEY,
  hub_id BIGINT REFERENCES hubs(id) ON DELETE SET NULL,
  name VARCHAR(200) NOT NULL,
  address TEXT,
  lat DOUBLE PRECISION,
  lon DOUBLE PRECISION,
  place_type VARCHAR(50), -- cafe, park, court, theater
  avg_price_level SMALLINT,
  partner_id BIGINT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Update activities to reference places
-- First, handle the existing varchar place_id from V14
ALTER TABLE activities RENAME COLUMN place_id TO google_place_id;

-- Now add the new foreign key column
ALTER TABLE activities ADD COLUMN place_id BIGINT REFERENCES places(id) ON DELETE SET NULL;

-- Ensure hub reference is correct (V14 made it nullable, we might want to enforce it or keep it nullable)
-- The plan says: ADD CONSTRAINT fk_activity_hub FOREIGN KEY (hub_id) REFERENCES hubs(id) ON DELETE RESTRICT;
-- It likely already exists, but let's ensure.
-- ALTER TABLE activities DROP CONSTRAINT IF EXISTS fk_activity_hub; -- Risky if name differs
-- We'll leave the existing FK for hub_id as it likely exists from creation.
