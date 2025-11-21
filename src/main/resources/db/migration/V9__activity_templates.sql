CREATE TABLE activity_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    duration_hours INTEGER,
    description TEXT,
    is_system_template BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    is_invite_only BOOLEAN DEFAULT FALSE,
    max_members INTEGER DEFAULT 4,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_templates_system ON activity_templates(is_system_template);
CREATE INDEX idx_activity_templates_user ON activity_templates(created_by_user_id);

-- Insert pre-built system templates
INSERT INTO activity_templates (name, title, category, duration_hours, description, is_system_template, is_invite_only, max_members) VALUES
('Coffee Meetup', 'Coffee at Galleria', 'FOOD', 1, 'Casual coffee meetup to chat and connect', TRUE, FALSE, 4),
('Pickup Basketball', 'Pickup Basketball Game', 'SPORTS', 2, 'Casual basketball game, all skill levels welcome', TRUE, FALSE, 8),
('Study Session', 'Study Group at Library', 'ART', 3, 'Focused study session with like-minded students', TRUE, FALSE, 6),
('Dinner Plans', 'Dinner at Local Restaurant', 'FOOD', 2, 'Group dinner to try out a new restaurant', TRUE, FALSE, 6),
('Live Music', 'Check out Live Music', 'MUSIC', 3, 'Attend a local live music performance', TRUE, FALSE, 4),
('Art Gallery Visit', 'Art Gallery Exploration', 'ART', 2, 'Visit and discuss art at a local gallery', TRUE, FALSE, 5);
