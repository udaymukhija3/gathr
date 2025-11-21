CREATE TABLE feedbacks (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    did_meet BOOLEAN NOT NULL,
    experience_rating INTEGER CHECK (experience_rating >= 1 AND experience_rating <= 5),
    would_hang_out_again BOOLEAN,
    added_to_contacts BOOLEAN DEFAULT FALSE,
    comments TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(activity_id, user_id)
);

CREATE INDEX idx_feedbacks_activity ON feedbacks(activity_id);
CREATE INDEX idx_feedbacks_user ON feedbacks(user_id);
