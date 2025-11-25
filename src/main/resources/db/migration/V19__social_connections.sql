CREATE TABLE IF NOT EXISTS social_connections (
    id BIGSERIAL PRIMARY KEY,
    source_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    connection_type VARCHAR(40) NOT NULL,
    strength NUMERIC(4,3) NOT NULL DEFAULT 0,
    interaction_count INTEGER NOT NULL DEFAULT 0,
    last_interacted_at TIMESTAMP,
    CONSTRAINT uq_social_connection UNIQUE (source_user_id, target_user_id, connection_type)
);

CREATE INDEX IF NOT EXISTS idx_social_connections_source
    ON social_connections(source_user_id, connection_type);

