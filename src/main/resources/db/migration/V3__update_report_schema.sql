-- Migration V3: Update Report table schema
-- Updates the report table to match the simplified schema requirements

-- Drop existing report table if it exists with old schema and recreate
DROP TABLE IF EXISTS report CASCADE;

CREATE TABLE report (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_id BIGINT REFERENCES activities(id) ON DELETE SET NULL,
    reason VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_report_reporter ON report(reporter_id);
CREATE INDEX idx_report_target_user ON report(target_user_id);
CREATE INDEX idx_report_activity ON report(activity_id);
CREATE INDEX idx_report_status ON report(status);
CREATE INDEX idx_report_created_at ON report(created_at);

COMMENT ON TABLE report IS 'User reports for moderation and safety';
COMMENT ON COLUMN report.status IS 'Report status: OPEN, CLOSED, etc.';

