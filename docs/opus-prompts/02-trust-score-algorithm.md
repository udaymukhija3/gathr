# Opus Prompt: Trust Score Algorithm Design

## Context

Building **Gathr** - a spontaneous activity discovery app where strangers meet for activities.

**Core Problem:** Users meet strangers. We need a trust/reputation system that:
- Helps users feel safe joining activities with unknown people
- Rewards reliable, positive community members
- Penalizes no-shows, bad behavior, harassment
- Is resistant to gaming/manipulation

**Stack:** Spring Boot 3 (Java 17) + PostgreSQL
**Scale:** 100K users

---

## Current Data Model

### Users
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100),
    trust_score INTEGER DEFAULT 50, -- Current: simple 0-100 score
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Activities & Participation
```sql
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    created_by BIGINT REFERENCES users(id),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) -- SCHEDULED, ACTIVE, COMPLETED, CANCELLED
);

CREATE TABLE participations (
    user_id BIGINT REFERENCES users(id),
    activity_id BIGINT REFERENCES activities(id),
    status VARCHAR(20), -- INTERESTED, CONFIRMED, LEFT
    attended BOOLEAN, -- Did they actually show up? (marked post-activity)
    joined_at TIMESTAMP
);
```

### Feedback System
```sql
CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT REFERENCES activities(id),
    from_user_id BIGINT REFERENCES users(id),
    to_user_id BIGINT REFERENCES users(id),
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Reports
```sql
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT REFERENCES users(id),
    target_user_id BIGINT REFERENCES users(id),
    activity_id BIGINT REFERENCES activities(id),
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, REVIEWED, ACTIONED, DISMISSED
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Contacts (Social Proof)
```sql
CREATE TABLE contacts (
    user_id BIGINT REFERENCES users(id),
    contact_hash VARCHAR(64),
    matched_user_id BIGINT -- If contact is on platform
);
```

---

## Task

Design a comprehensive **Trust Score Algorithm** that calculates a 0-100 score for each user.

### Input Signals Available

| Signal | Source | Notes |
|--------|--------|-------|
| Activities attended | participations.attended = true | Positive signal |
| No-shows | CONFIRMED but attended = false | Strong negative |
| Ratings received | feedback.rating (1-5 stars) | Weighted by recency |
| Reports against user | reports.target_user_id | Varies by severity |
| Account age | users.created_at | Older = more trusted |
| Activities created | activities.created_by | Shows engagement |
| Contacts uploaded | contacts count | Social proof |
| Mutual connections | Matched contacts | Strong trust signal |
| Cancellation rate | Cancelled own activities | Negative if high |

### Requirements

1. **Score Range:** 0-100
   - 0-30: Low trust (restricted features)
   - 31-60: Normal trust
   - 61-80: Good trust
   - 81-100: Excellent trust (badges, priority)

2. **Manipulation Resistance:**
   - Can't farm score by creating/joining fake activities
   - Can't boost friends' scores artificially
   - Sudden score changes should be flagged

3. **Time Decay:**
   - Recent behavior weighted more than old
   - Bad behavior from 1 year ago matters less
   - But severe violations (harassment) have longer memory

4. **Penalty Severity:**
   - No-show: Moderate penalty
   - Cancelling activity last-minute: Small penalty
   - Report (harassment): Severe penalty
   - Report (spam): Moderate penalty
   - Report dismissed (false report): Penalty to reporter

5. **Recovery Path:**
   - Users with low scores should be able to recover
   - Clear path: attend X activities, get Y positive ratings
   - But repeated offenders have slower recovery

6. **Feature Gating:**
   - Score < 30: Can't create activities, can only join
   - Score < 50: Can't create invite-only activities
   - Score < 40: Warning shown to others

---

## Specific Questions to Address

1. **Formula Design:**
   - How to combine multiple signals into single score?
   - Additive? Multiplicative? Weighted average?
   - How to handle missing signals (new user, no feedback yet)?

2. **Weights:**
   - What weight for each signal?
   - How to determine weights without ML training data?
   - Should weights be configurable?

3. **Time Decay:**
   - Exponential decay? Linear decay?
   - Different decay rates for positive vs negative signals?
   - Half-life for different event types?

4. **Anti-Gaming:**
   - How to detect coordinated boosting (friends rating each other)?
   - How to detect fake activities for score farming?
   - Rate limiting on score-affecting actions?

5. **Edge Cases:**
   - User with 1 activity, 1 perfect rating = 100 score? (No - need minimum activity)
   - User reported once unfairly = score tanks? (No - pending review doesn't affect)
   - Inactive user for 6 months - score unchanged or decays?

6. **Computation:**
   - Calculate on-demand or pre-compute?
   - How often to recalculate?
   - Database query performance?

---

## Constraints

- Must work with PostgreSQL (no Redis required, but can suggest)
- No ML models - must be deterministic formula
- Must be auditable (explain why score is X)
- Score changes should be logged for disputes

---

## Expected Output

### 1. High-Level Approach
Philosophy behind the scoring system. Trade-offs considered.

### 2. Mathematical Formula
```
TrustScore = f(attendance, ratings, reports, age, social_proof, ...)
```
With exact weights and normalization.

### 3. Component Breakdown
For each input signal:
- How to normalize (0-1 scale)
- Weight in final formula
- Time decay function
- Edge case handling

### 4. Database Schema Changes
```sql
-- New tables for score history, event log, etc.
CREATE TABLE trust_score_events (
    ...
);
```

### 5. Calculation Algorithm
```
function calculateTrustScore(userId):
    // Step-by-step with SQL queries
```

### 6. Anti-Gaming Rules
Specific rules to prevent manipulation with examples.

### 7. Feature Gating Matrix
| Score Range | Can Create Activity | Can Create Invite-Only | Shown Warning | Badges |
|-------------|--------------------|-----------------------|---------------|--------|
| 0-30        | No                 | No                    | Yes           | None   |
| ...         | ...                | ...                   | ...           | ...    |

### 8. Score Recovery Paths
For a user at score 25, what actions move them to 50?

### 9. Edge Cases
List of 10+ edge cases and how formula handles them.

### 10. Audit Trail
How to explain to user: "Your score dropped because..."

---

## Example Scenarios

Please walk through score calculation for:

1. **New User (Day 1):**
   - Just signed up, uploaded contacts, no activity yet

2. **Active Good User:**
   - 20 activities attended, 4.5 avg rating, 0 reports, 6 months old

3. **No-Show User:**
   - 10 activities, 3 no-shows, 3.0 avg rating, 1 month old

4. **Reported User:**
   - 15 activities, 1 harassment report (actioned), 4.0 avg rating

5. **Inactive Recovered User:**
   - Was at 25 score, inactive 3 months, now attending activities again

---

## Success Criteria

A good solution will:
- Be fair (similar behavior = similar scores)
- Be explainable (can tell user why their score is X)
- Be resistant to gaming (can't easily boost score)
- Allow recovery (bad actors can redeem with good behavior)
- Be computationally efficient (calculate in <100ms)
- Degrade gracefully (missing data doesn't break formula)
