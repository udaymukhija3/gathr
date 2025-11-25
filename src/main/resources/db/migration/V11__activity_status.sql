-- Add status field to activities for explicit lifecycle tracking
-- Status can be: SCHEDULED, ACTIVE, COMPLETED, CANCELLED

ALTER TABLE activities ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED';

-- Set existing activities to appropriate status based on time
UPDATE activities
SET status = CASE
    WHEN end_time < NOW() THEN 'COMPLETED'
    WHEN start_time <= NOW() AND end_time >= NOW() THEN 'ACTIVE'
    ELSE 'SCHEDULED'
END;

-- Create index for status queries
CREATE INDEX idx_activities_status ON activities(status);
