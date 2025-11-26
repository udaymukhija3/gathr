-- Justification: Core user data, minimal PII
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100),
    bio TEXT,
    gender VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Justification: The core unit of the app
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    creator_id BIGINT REFERENCES users(id),
    hub_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    max_participants INT DEFAULT 4,
    status VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Justification: Many-to-many relationship for joining
CREATE TABLE participation (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT REFERENCES activities(id),
    user_id BIGINT REFERENCES users(id),
    status VARCHAR(20) NOT NULL, -- INTERESTED, CONFIRMED
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(activity_id, user_id)
);

-- Justification: Safety - blocking is essential
CREATE TABLE blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT REFERENCES users(id),
    blocked_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id)
);

-- Justification: Safety - reporting bad actors
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT REFERENCES users(id),
    reported_id BIGINT REFERENCES users(id), -- nullable if reporting activity
    activity_id BIGINT REFERENCES activities(id), -- nullable if reporting user
    reason VARCHAR(50) NOT NULL,
    details TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Justification: Trust signal - privacy preserving
CREATE TABLE contact_hashes (
    user_id BIGINT REFERENCES users(id),
    phone_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, phone_hash)
);
