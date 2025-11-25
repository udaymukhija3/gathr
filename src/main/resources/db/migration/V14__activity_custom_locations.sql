ALTER TABLE activities
    ADD COLUMN place_id VARCHAR(191),
    ADD COLUMN place_name VARCHAR(255),
    ADD COLUMN place_address TEXT,
    ADD COLUMN latitude DECIMAL(10, 8),
    ADD COLUMN longitude DECIMAL(11, 8),
    ADD COLUMN is_user_location BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE activities
    ALTER COLUMN hub_id DROP NOT NULL;

-- Backfill existing activities with their hub metadata
UPDATE activities a
SET place_name = h.name,
    place_address = h.area,
    latitude = h.latitude,
    longitude = h.longitude,
    is_user_location = FALSE
FROM hubs h
WHERE a.hub_id = h.id
  AND (a.place_name IS NULL OR a.latitude IS NULL OR a.longitude IS NULL);

CREATE INDEX IF NOT EXISTS idx_activities_lat_long
    ON activities (latitude, longitude);

