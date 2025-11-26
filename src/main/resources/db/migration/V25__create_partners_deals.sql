-- Create partners table
CREATE TABLE IF NOT EXISTS partners (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  contact_info JSONB,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Create venue_deals table
CREATE TABLE IF NOT EXISTS venue_deals (
  id BIGSERIAL PRIMARY KEY,
  partner_id BIGINT REFERENCES partners(id) ON DELETE CASCADE,
  place_id BIGINT REFERENCES places(id) ON DELETE CASCADE,
  description TEXT,
  discount_percent SMALLINT,
  valid_from TIMESTAMP,
  valid_until TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);
