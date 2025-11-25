# Opus Prompt: Social Graph Analysis for Activity Discovery

## Context

Building **Gathr** - a spontaneous activity discovery app where the social graph (who knows whom) is a critical signal for recommendations and trust.

**Key Insight:** People are more likely to join activities where:
- Their direct contacts are going (1st degree)
- Friends-of-friends are going (2nd degree)
- People from their "community cluster" are going

**Stack:** Spring Boot 3 (Java 17) + PostgreSQL
**Scale:** 100K users, potentially 10M+ contact relationships

---

## Current Data Model

### Contacts (Privacy-Preserving)
```sql
CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),      -- User who uploaded
    contact_hash VARCHAR(64) NOT NULL,         -- SHA-256(normalized_phone)
    matched_user_id BIGINT REFERENCES users(id), -- Non-null if contact is on platform
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, contact_hash)
);

-- Indexes
CREATE INDEX idx_contacts_user ON contacts(user_id);
CREATE INDEX idx_contacts_matched ON contacts(matched_user_id);
CREATE INDEX idx_contacts_hash ON contacts(contact_hash);
```

### Mutual Connection Logic
```sql
-- A and B are "mutuals" if:
-- A has B's phone hash in contacts AND B has A's phone hash in contacts
-- This means both saved each other's numbers
```

### Participations
```sql
CREATE TABLE participations (
    user_id BIGINT REFERENCES users(id),
    activity_id BIGINT REFERENCES activities(id),
    status VARCHAR(20), -- INTERESTED, CONFIRMED
    joined_at TIMESTAMP
);
```

### Co-Attendance History
```sql
-- Implicit: Users who attended same activities together
-- Can be derived from participations table
```

---

## Task

Design **social graph analysis algorithms** for Gathr that power:

1. **Mutual Count per Activity** - How many of my contacts are attending?
2. **Friend-of-Friend Discovery** - Activities where 2nd-degree connections are going
3. **Community Detection** - Identify clusters/groups within users
4. **"You Might Know"** - Suggest connections based on co-attendance
5. **Bridge User Detection** - Users who connect different social circles

---

## Part 1: Mutual Count (Currently Implemented - Optimize)

### Current Query (Slow)
```sql
-- For each activity, count user's mutuals attending
SELECT COUNT(*)
FROM participations p
JOIN contacts c1 ON c1.matched_user_id = p.user_id  -- Contact is participant
WHERE c1.user_id = :currentUserId                    -- My contact
AND p.activity_id = :activityId
AND EXISTS (                                          -- They also have me
    SELECT 1 FROM contacts c2
    WHERE c2.user_id = p.user_id
    AND c2.matched_user_id = :currentUserId
);
```

### Problem
- N+1 query problem when loading feed (query per activity)
- Full table scans on large contact tables
- Need to compute for 50+ activities in feed

### Questions
1. How to pre-compute/cache mutual relationships?
2. Materialized view? Separate mutuals table?
3. How to efficiently get mutual count for batch of activities?

---

## Part 2: Friend-of-Friend (2nd Degree) Discovery

### Goal
Show activities where friends-of-friends are attending, even if no direct mutuals.

### Example
```
User A → knows → User B → knows → User C
User C is attending "Basketball Tonight"
Show to User A: "Friend of a friend is going"
```

### Challenges
1. 2nd degree can explode (1000 contacts × 1000 contacts = 1M)
2. Need to weight by connection strength
3. Performance at scale

### Questions
1. How to efficiently query 2nd-degree connections?
2. Should we limit depth? (only 2nd degree, not 3rd)
3. How to rank 2nd-degree activities?
4. Privacy: Should we reveal who the friend-of-friend is?

---

## Part 3: Community Detection

### Goal
Identify natural clusters/groups among users based on:
- Mutual connections (contact graph)
- Co-attendance (activity participation graph)
- Geographic proximity (same hub)
- Interest overlap

### Use Cases
1. "Popular in your community" - Activities trending in user's cluster
2. Hub recommendations - "People like you prefer Cyberhub"
3. Warm intros - "You're both in the Gurgaon Sports community"

### Questions
1. What algorithm for community detection? (Louvain? Label Propagation?)
2. Can this run in PostgreSQL or need external processing?
3. How often to recompute clusters?
4. How many clusters per user? (User can be in multiple)

---

## Part 4: "You Might Know" (Connection Suggestions)

### Goal
Suggest users to connect with based on:
- Co-attendance at activities (attended 3+ same activities)
- Mutual friends (many shared connections)
- Similar interests + same hub
- Friend-of-friend who they interacted with in chat

### Example
```
"You might know Sarah"
- Attended 4 activities together
- 5 mutual connections
- Both interested in Sports, Food
```

### Questions
1. Scoring formula for connection suggestions?
2. Privacy considerations (can't reveal too much about non-connections)
3. How to handle: they interacted but didn't save contact?
4. Rate limiting (don't spam suggestions)

---

## Part 5: Bridge User Detection

### Goal
Identify "connector" users who bridge different social circles.

### Why It Matters
- Bridge users are valuable for community growth
- They introduce people from different groups
- Activities created by bridge users attract diverse crowds

### Definition
A bridge user has connections in multiple distinct clusters, where those clusters have few connections between them.

### Use Cases
1. Feature bridge users as "Community Connectors"
2. Recommend bridge user activities for diversity
3. Analyze community health (are clusters isolated?)

### Questions
1. Formal definition of "bridge" in graph terms?
2. How to compute efficiently?
3. What metrics to track per user?

---

## Performance Constraints

- 100K users
- Average 200 contacts per user = 20M contact rows
- Average 10 activities per user = 1M participation rows
- Feed load must be <200ms (P95)
- Graph queries must be batched

---

## Expected Output

### 1. Data Model Enhancements
```sql
-- New tables for pre-computed graph data
CREATE TABLE user_mutuals (
    user_id BIGINT,
    mutual_user_id BIGINT,
    strength INTEGER, -- Number of shared contacts, co-attendances
    last_interaction TIMESTAMP,
    PRIMARY KEY (user_id, mutual_user_id)
);

CREATE TABLE user_communities (
    user_id BIGINT,
    community_id INTEGER,
    membership_score FLOAT,
    ...
);
```

### 2. Efficient Mutual Count Query
Optimized query or pre-computation strategy for "mutuals attending activity X"

### 3. Friend-of-Friend Algorithm
```
function getFoFActivities(userId, hubId, date):
    // Pseudocode with complexity analysis
```

### 4. Community Detection Pipeline
- Algorithm choice with justification
- SQL/code for clustering
- Update frequency and triggers

### 5. Connection Suggestion Scoring
```
SuggestionScore = f(co_attendance, mutual_friends, interest_overlap, recency)
```

### 6. Bridge User Metrics
- Betweenness centrality approximation
- SQL query to identify top bridges

### 7. Caching Strategy
What to cache, TTLs, invalidation

### 8. Batch Processing
For operations that can't be real-time, define batch jobs

### 9. Privacy Safeguards
What to reveal vs hide in each feature

### 10. Query Performance
Expected query times, indexes needed, explain plans

---

## Example Scenarios

### Scenario 1: Feed Load
User opens app. Need to show 20 activities with mutual counts.
Walk through the optimal query path.

### Scenario 2: New Activity Created
User creates "Basketball Tonight".
How do we notify/recommend to 2nd-degree connections?

### Scenario 3: Community Event
"Cyberhub Sports Community" has 500 members.
How do we identify them and show community-specific feed?

### Scenario 4: Connection Suggestion
User attended 5 activities with same person, no contact saved.
When and how do we suggest connecting?

---

## Constraints

- PostgreSQL only (no Neo4j/graph DB)
- No real-time streaming (batch is OK)
- Must be implementable without graph database expertise
- Privacy: Never expose full contact lists

---

## Success Criteria

A good solution will:
- Handle 100K users without dedicated graph infrastructure
- Provide mutual counts in <50ms per activity batch
- Identify meaningful communities (not random clusters)
- Respect privacy (never leak contact information)
- Be maintainable by non-graph-experts
