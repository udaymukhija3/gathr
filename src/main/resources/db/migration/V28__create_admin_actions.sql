-- Create admin_actions table
CREATE TABLE IF NOT EXISTS admin_actions (
  id BIGSERIAL PRIMARY KEY,
  report_id BIGINT REFERENCES reports(id) ON DELETE SET NULL,
  action_taken VARCHAR(100),
  actor_id BIGINT REFERENCES users(id),
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);
