-- Create event_logs table
CREATE TABLE IF NOT EXISTS event_logs (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
  event_type VARCHAR(80) NOT NULL,
  event_time TIMESTAMP DEFAULT NOW(),
  payload JSONB,
  user_agent TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_logs_event_type_time ON event_logs(event_type, event_time);
CREATE INDEX IF NOT EXISTS idx_event_logs_user_time ON event_logs(user_id, event_time);
