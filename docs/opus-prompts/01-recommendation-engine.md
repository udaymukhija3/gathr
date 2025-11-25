# Opus Prompt: Recommendation Engine Architecture

## Context

Building **Gathr** - a spontaneous activity discovery app competing with Misfits in India.

**Stack:** Spring Boot 3 (Java 17) + PostgreSQL + React Native (Expo)
**Scale target:** 100K users, Delhi NCR initially, expanding to other metros

**Key Differentiator:** Unlike Misfits (club-based, weekly commitment), Gathr is about "what's happening tonight" - spontaneous, low-commitment activities.

---

## Current Data Model

### Users
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100),
    bio TEXT,
    avatar_url VARCHAR(500),
    home_hub_id BIGINT REFERENCES hubs(id),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    trust_score INTEGER DEFAULT 50,
    onboarding_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_interests (
    user_id BIGINT REFERENCES users(id),
    interest VARCHAR(50), -- SPORTS, FOOD, ART, MUSIC, OUTDOOR, GAMES, LEARNING, WELLNESS
    PRIMARY KEY (user_id, interest)
);
```

### Activities
```sql
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    hub_id BIGINT REFERENCES hubs(id),
    category VARCHAR(50), -- Same 8 categories as interests
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id),
    is_invite_only BOOLEAN DEFAULT FALSE,
    max_members INTEGER,
    status VARCHAR(20) DEFAULT 'SCHEDULED', -- SCHEDULED, ACTIVE, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE participations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    activity_id BIGINT REFERENCES activities(id),
    status VARCHAR(20), -- INTERESTED, CONFIRMED, LEFT
    joined_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, activity_id)
);
```

### Social Graph (Contacts)
```sql
CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    contact_hash VARCHAR(64), -- SHA-256 of normalized phone
    matched_user_id BIGINT REFERENCES users(id), -- NULL if contact not on platform
    created_at TIMESTAMP DEFAULT NOW()
);
-- Mutual = both users have each other's contact_hash
```

### Hubs (Locations)
```sql
CREATE TABLE hubs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100),
    area VARCHAR(200),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    description TEXT
);
```

---

## Task

Design a **recommendation engine** for Gathr's "For You" feed that ranks activities by relevance to each user.

### Requirements

1. **Ranking Signals** (in rough priority order):
   - Interest match (user's interests vs activity category)
   - Mutual connections (friends/contacts attending)
   - Past behavior (categories user has joined before)
   - Time preferences (user's preferred times based on history)
   - Location proximity (distance from user to hub)
   - Activity popularity (join velocity, spots remaining)
   - Creator trust score (higher trust = boost)
   - Recency (recently created activities get temporary boost)

2. **Cold Start Handling:**
   - New users with no history: Fall back to interests + location
   - New users with no interests: Show popular in their hub
   - New activities with no participants: Boost briefly to get initial traction

3. **Explainability:**
   - Each recommendation should have a "reason" the frontend can display
   - Examples: "Matches your Sports interest", "2 friends are going", "Popular in Cyberhub"

4. **Diversity:**
   - Don't show only one category (even if user loves Sports)
   - Mix familiar (interest match) with exploratory (new categories)
   - Balance activities by time slots (don't show only 7 PM activities)

5. **Performance Constraints:**
   - Must return ranked feed in <200ms for 95th percentile
   - 100K users, ~500 activities per hub per day
   - Feed is personalized per user (no global ranking)

---

## Specific Questions to Address

1. **Scoring Formula:**
   - What's the mathematical formula for combining signals?
   - How to normalize different signals to same scale?
   - How to weight signals? Fixed weights vs learned?

2. **Architecture:**
   - Real-time computation vs pre-computed scores?
   - Caching strategy (user scores, activity features)?
   - When to invalidate/refresh recommendations?

3. **Data Pipeline:**
   - What derived tables/materialized views are needed?
   - How to efficiently compute "mutuals attending activity X"?
   - Batch vs streaming updates?

4. **Exploration vs Exploitation:**
   - How to introduce diversity without hurting relevance?
   - Epsilon-greedy? Thompson sampling? Or simpler heuristics?

5. **Feedback Loop:**
   - How to track if recommendations worked (user joined)?
   - How to avoid filter bubbles?

---

## Constraints

- **No external ML services** - must be self-contained in PostgreSQL + Java
- **No training data yet** - can't train models, need heuristic approach that can evolve
- Must integrate with existing schema (provide ALTER TABLE if needed)
- API must be REST (existing pattern in codebase)

---

## Expected Output

### 1. High-Level Approach (2-3 paragraphs)
Explain the overall strategy, trade-offs considered, and why this approach.

### 2. Scoring Formula
Mathematical formula with weights for each signal. Explain normalization.

### 3. Database Schema Changes
```sql
-- Any new tables, indexes, materialized views needed
```

### 4. Algorithm Pseudocode
```
function getRecommendations(userId, hubId, date, limit):
    // Step-by-step logic
```

### 5. API Contract
```json
// Request
GET /feed/recommendations?hub_id=1&date=2024-11-24&limit=20

// Response
{
  "activities": [...],
  "reasons": {
    "123": {
      "primary": "Matches your Sports interest",
      "secondary": "2 friends are going",
      "score": 0.87
    }
  }
}
```

### 6. Caching Strategy
What to cache, TTLs, invalidation triggers.

### 7. Cold Start Handling
Specific logic for each cold start scenario.

### 8. Edge Cases & Failure Modes
What can go wrong? How to handle gracefully?

### 9. Future Improvements
What to add when we have more data/resources?

---

## Success Criteria

A good solution will:
- Be implementable in 1-2 weeks by a single developer
- Not require ML expertise to maintain
- Degrade gracefully (if signals missing, still works)
- Be explainable to users ("why am I seeing this?")
- Scale to 100K users without infrastructure changes
