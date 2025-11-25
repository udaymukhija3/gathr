-- Activity-level metrics to support real-time-ish feed ranking

CREATE TABLE activity_metrics (
    activity_id     BIGINT PRIMARY KEY,
    join_count_1hr  INTEGER     DEFAULT 0,
    total_joins     INTEGER     DEFAULT 0,
    view_count      INTEGER     DEFAULT 0,
    last_join_at    TIMESTAMP,
    CONSTRAINT fk_activity_metrics_activity
        FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_metrics_last_join ON activity_metrics(last_join_at);


