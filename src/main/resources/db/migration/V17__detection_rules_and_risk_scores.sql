-- Basic fraud & abuse detection scaffolding

CREATE TABLE detection_rules (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(50)  NOT NULL, -- FAKE_ACCOUNT, SPAM, COORDINATION, HARASSMENT
    condition_type  VARCHAR(50)  NOT NULL,
    condition_params JSONB       NOT NULL,
    risk_delta      INTEGER      NOT NULL,
    action          VARCHAR(50)  NOT NULL, -- FLAG, RESTRICT, BAN, NONE
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE user_risk_scores (
    user_id     BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    base_score  INTEGER      NOT NULL DEFAULT 0,
    behavior_score INTEGER   NOT NULL DEFAULT 0,
    report_score   INTEGER   NOT NULL DEFAULT 0,
    coordination_score INTEGER NOT NULL DEFAULT 0,
    total_score INTEGER      NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);


