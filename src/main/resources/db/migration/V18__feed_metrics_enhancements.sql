-- Feed personalization support columns

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS preferred_radius_km INTEGER DEFAULT 10;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS home_latitude DECIMAL(10, 8);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS home_longitude DECIMAL(11, 8);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_active TIMESTAMP;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS preferred_start_hour INTEGER;

-- Enhance feed metrics table to capture interaction level telemetry

ALTER TABLE feed_metrics DROP CONSTRAINT IF EXISTS feed_metrics_pkey;

ALTER TABLE feed_metrics
    ADD COLUMN IF NOT EXISTS id BIGSERIAL PRIMARY KEY;

ALTER TABLE feed_metrics
    ADD COLUMN IF NOT EXISTS hub_id BIGINT REFERENCES hubs(id);

ALTER TABLE feed_metrics
    ADD COLUMN IF NOT EXISTS action VARCHAR(20);

ALTER TABLE feed_metrics
    ADD COLUMN IF NOT EXISTS action_timestamp TIMESTAMP DEFAULT NOW();

ALTER TABLE feed_metrics
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(100);

ALTER TABLE feed_metrics
    RENAME COLUMN shown_at TO created_at;

ALTER TABLE feed_metrics
    ALTER COLUMN created_at SET DEFAULT NOW();

ALTER TABLE feed_metrics
    DROP COLUMN IF EXISTS clicked,
    DROP COLUMN IF EXISTS joined,
    DROP COLUMN IF EXISTS time_to_click;

CREATE INDEX IF NOT EXISTS idx_feed_metrics_user_created
    ON feed_metrics(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_feed_metrics_action
    ON feed_metrics(user_id, action);

CREATE INDEX IF NOT EXISTS idx_feed_metrics_session
    ON feed_metrics(session_id);

