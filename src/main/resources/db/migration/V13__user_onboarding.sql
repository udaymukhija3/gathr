-- User Onboarding & Location Support
-- Enables proper onboarding flow with interests and location detection

-- =====================================================
-- USER PROFILE ENHANCEMENTS
-- =====================================================

-- Onboarding status
ALTER TABLE users ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;

-- User interests (activity categories)
ALTER TABLE users ADD COLUMN interests TEXT[]; -- Array of: SPORTS, FOOD, ART, MUSIC

-- User bio/about
ALTER TABLE users ADD COLUMN bio VARCHAR(500);

-- Profile photo URL
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(512);

-- User's current location
ALTER TABLE users ADD COLUMN latitude DECIMAL(10, 8);
ALTER TABLE users ADD COLUMN longitude DECIMAL(11, 8);
ALTER TABLE users ADD COLUMN location_updated_at TIMESTAMP;

-- User's preferred/home hub
ALTER TABLE users ADD COLUMN home_hub_id BIGINT REFERENCES hubs(id);

-- Create index for location queries
CREATE INDEX idx_users_location ON users(latitude, longitude) WHERE latitude IS NOT NULL;
CREATE INDEX idx_users_home_hub ON users(home_hub_id) WHERE home_hub_id IS NOT NULL;
CREATE INDEX idx_users_onboarding ON users(onboarding_completed);

-- =====================================================
-- HUB LOCATION ENHANCEMENTS
-- =====================================================

-- Add coordinates to hubs for distance calculation
ALTER TABLE hubs ADD COLUMN latitude DECIMAL(10, 8);
ALTER TABLE hubs ADD COLUMN longitude DECIMAL(11, 8);

-- Create spatial index for hub discovery
CREATE INDEX idx_hubs_location ON hubs(latitude, longitude) WHERE latitude IS NOT NULL;

-- =====================================================
-- SEED DATA: Update existing hubs with Gurgaon coordinates
-- =====================================================

-- Cyber Hub area
UPDATE hubs SET latitude = 28.4947, longitude = 77.0888 WHERE area ILIKE '%cyber%' OR name ILIKE '%cyber%';

-- Golf Course Road area
UPDATE hubs SET latitude = 28.4595, longitude = 77.0724 WHERE area ILIKE '%golf%' OR name ILIKE '%golf%';

-- DLF Phase areas
UPDATE hubs SET latitude = 28.4729, longitude = 77.0855 WHERE area ILIKE '%dlf%' OR name ILIKE '%dlf%';

-- Sector 29 (food/nightlife hub)
UPDATE hubs SET latitude = 28.4690, longitude = 77.0643 WHERE area ILIKE '%sector 29%' OR name ILIKE '%sector 29%';

-- MG Road
UPDATE hubs SET latitude = 28.4799, longitude = 77.0827 WHERE area ILIKE '%mg road%' OR name ILIKE '%mg%';

-- Default Gurgaon center for any remaining hubs
UPDATE hubs SET latitude = 28.4595, longitude = 77.0266 WHERE latitude IS NULL;
