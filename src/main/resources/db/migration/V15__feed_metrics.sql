-- Feed metrics for recommendation/debugging
-- Tracks which activities were shown to which user, with score and interaction flags

CREATE TABLE feed_metrics (
    user_id     BIGINT      NOT NULL,
    activity_id BIGINT      NOT NULL,
    position    INTEGER     NOT NULL,
    score       NUMERIC(5,3),
    shown_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    clicked     BOOLEAN     DEFAULT FALSE,
    joined      BOOLEAN     DEFAULT FALSE,
    time_to_click INTEGER,
    PRIMARY KEY (user_id, activity_id, shown_at),
    CONSTRAINT fk_feed_metrics_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_metrics_activity
        FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE
);

CREATE INDEX idx_feed_metrics_user ON feed_metrics(user_id);
CREATE INDEX idx_feed_metrics_activity ON feed_metrics(activity_id);


