-- Migration V5: Create Events table
-- Creates the events table for universal event logging

CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    activity_id BIGINT,
    event_type VARCHAR(64) NOT NULL,
    properties JSONB,
    ts TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id);
CREATE INDEX IF NOT EXISTS idx_events_activity_id ON events(activity_id);
CREATE INDEX IF NOT EXISTS idx_events_event_type ON events(event_type);
CREATE INDEX IF NOT EXISTS idx_events_ts ON events(ts);

COMMENT ON TABLE events IS 'Universal event logging for instrumentation';
COMMENT ON COLUMN events.properties IS 'JSON properties for event-specific data';
COMMENT ON COLUMN events.event_type IS 'Event type: activity_created, activity_joined, etc.';

