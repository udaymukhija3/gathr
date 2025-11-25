# Opus Prompt: Real-Time Feed Ranking System

## Context

Building **Gathr** - a spontaneous activity discovery app where the feed needs to feel "alive" and reflect real-time popularity.

**Problem:** Static feed ranking feels stale. Users want to see:
- Activities gaining momentum ("🔥 5 people joined in last hour")
- Urgency signals ("Only 2 spots left!", "Starts in 30 min")
- Social proof in real-time ("Sarah just joined")

**Stack:** Spring Boot 3 (Java 17) + PostgreSQL + WebSocket (already implemented for chat)
**Scale:** 1000 concurrent users, 500 activities per hub per day

---

## Current State

### Feed Loading (Current - Static)
```java
// ActivityService.java
public List<Activity> getActivitiesByHub(Long hubId, LocalDate date) {
    return activityRepository.findByHubIdAndDate(hubId, date);
    // Returns in creation order, no ranking
}
```

### WebSocket (Already Have)
```java
// WebSocketMessageController.java
// Currently used for chat messages
// Can extend for real-time feed updates
```

### Activity Data
```sql
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    hub_id BIGINT,
    category VARCHAR(50),
    start_time TIMESTAMP,
    max_members INTEGER,
    created_by BIGINT,
    created_at TIMESTAMP
);

CREATE TABLE participations (
    user_id BIGINT,
    activity_id BIGINT,
    status VARCHAR(20), -- INTERESTED, CONFIRMED
    joined_at TIMESTAMP
);
```

---

## Task

Design a **real-time feed ranking system** that:

1. **Ranks activities dynamically** based on real-time signals
2. **Pushes updates** to connected clients when rankings change significantly
3. **Shows engagement velocity** ("5 joined in last hour")
4. **Handles urgency** (time until start, spots remaining)
5. **Balances freshness vs relevance** (new activities get temporary boost)

---

## Ranking Signals (Real-Time)

### Time-Sensitive Signals
| Signal | Update Frequency | Weight Change |
|--------|-----------------|---------------|
| Time until start | Every minute | Increases as event approaches |
| Spots remaining | On each join | Increases when few spots left |
| Join velocity | Rolling 1hr window | High velocity = boost |
| Activity age | Once per hour | New activities get boost that decays |

### Engagement Signals
| Signal | Update Frequency | Notes |
|--------|-----------------|-------|
| Total participants | On join/leave | Base popularity |
| Confirmed vs Interested ratio | On status change | Higher confirm = more serious |
| Chat activity | On message | Active discussion = boost |
| Views (if tracked) | On view | Interest without commitment |

### Quality Signals (Less Dynamic)
| Signal | Update Frequency | Notes |
|--------|-----------------|-------|
| Creator trust score | Daily | Higher trust = boost |
| Category match to user | On user interest change | Personalization |
| Mutual connections attending | On join by mutual | Social proof |

---

## Requirements

### 1. Score Calculation
```
ActivityScore(t) = f(
    time_until_start,
    spots_remaining,
    join_velocity_1hr,
    total_participants,
    creator_trust,
    age_boost,
    user_personalization
)
```

Score should:
- Update in near-real-time (within 30 seconds of change)
- Be comparable across activities (normalized 0-100)
- Support sorting/filtering efficiently

### 2. Real-Time Updates
When should we push feed updates to clients?
- Participant count changes by ≥2
- Activity moves from "spots available" to "almost full"
- New activity created in user's hub
- Activity cancelled

### 3. Velocity Calculation
```
JoinVelocity = (joins in last 60 min) / (expected baseline for this time/category)
```
- "🔥 Trending" badge if velocity > 2x baseline
- Show "X joined in last hour" for high-velocity activities

### 4. Urgency Signals
```
if (spots_remaining <= 3 && spots_remaining > 0):
    show "Only X spots left!" (orange badge)

if (spots_remaining == 0):
    show "Full" (red badge) OR "Waitlist available"

if (time_until_start <= 60 min):
    show "Starting soon!" (boost ranking)
```

### 5. Freshness Boost
New activities should get visibility even with 0 participants:
```
FreshnessBoost = decay_function(age_in_hours)
// Full boost for first 2 hours, then decay to 0 by 12 hours
```

---

## Architecture Questions

### 1. Where to Compute Scores?

**Option A: Database (Materialized View)**
```sql
CREATE MATERIALIZED VIEW activity_scores AS
SELECT
    a.id,
    calculate_score(a.id, ...) as score
FROM activities a
WHERE a.status = 'SCHEDULED';

-- Refresh periodically
REFRESH MATERIALIZED VIEW activity_scores;
```
- Pro: SQL-native, easy to query
- Con: Not truly real-time, refresh lag

**Option B: Application Layer (In-Memory)**
```java
// Cache scores in memory, update on events
Map<Long, ActivityScore> scoreCache;

@EventListener
public void onParticipationChange(ParticipationEvent e) {
    recalculateScore(e.getActivityId());
    broadcastIfSignificant();
}
```
- Pro: True real-time
- Con: Memory overhead, cache invalidation complexity

**Option C: Redis**
```
ZADD activity_scores:hub:1 0.85 "activity:123"
ZADD activity_scores:hub:1 0.72 "activity:456"
```
- Pro: Fast sorted sets, TTL support
- Con: Additional infrastructure

**Which approach? Justify trade-offs.**

### 2. How to Handle 1000 Concurrent Users?

- Each user has personalized feed (interest weights differ)
- Can't compute 1000 × 500 = 500K scores per second
- Need caching strategy

### 3. WebSocket Push Strategy

- Push every score change? (Too noisy)
- Push on significant changes only? (Define "significant")
- Batch updates every X seconds?
- Only push for activities user is viewing?

### 4. Cold Start for New Activities

- New activity has 0 participants, no velocity
- Should still appear in feed
- How long does freshness boost last?
- What if no one joins in 6 hours? (Remove from feed? Show at bottom?)

---

## Expected Output

### 1. Scoring Formula
```
Score(activity, user, time) =
    w1 * interest_match(user, activity.category) +
    w2 * time_urgency(activity.start_time, time) +
    w3 * scarcity(activity.spots_remaining, activity.max_members) +
    w4 * velocity(activity.joins_last_hour, baseline) +
    w5 * social_proof(mutuals_attending) +
    w6 * freshness(activity.created_at, time) +
    w7 * creator_trust(activity.creator.trust_score)
```

With specific functions for each component.

### 2. Architecture Decision
Recommended approach (DB/App/Redis) with justification.

### 3. Data Model Changes
```sql
-- Any new tables/columns needed
CREATE TABLE activity_metrics (
    activity_id BIGINT PRIMARY KEY,
    current_score FLOAT,
    join_count_1hr INTEGER,
    last_join_at TIMESTAMP,
    view_count INTEGER,
    updated_at TIMESTAMP
);
```

### 4. Score Update Pipeline
```
Event: User joins activity
→ Update participation table
→ Increment join_count_1hr
→ Recalculate score
→ If score_change > threshold:
    → Broadcast to hub subscribers
```

### 5. WebSocket Protocol
```json
// Server → Client
{
    "type": "FEED_UPDATE",
    "hubId": 1,
    "updates": [
        {
            "activityId": 123,
            "newScore": 0.87,
            "joinCount": 8,
            "velocity": "trending",
            "spotsLeft": 2
        }
    ]
}
```

### 6. Caching Strategy
- What to cache
- TTLs
- Invalidation triggers
- Memory estimates

### 7. Batch Job for Cleanup
- Remove stale metrics
- Recalculate baseline velocities
- Archive completed activity data

### 8. Client-Side Handling
- How should React Native handle feed updates?
- Optimistic updates vs server confirmation
- Animation for score changes

### 9. Fallback Strategy
- What if real-time system fails?
- Graceful degradation to static ranking

### 10. Monitoring
- Key metrics to track
- Alerts for system health

---

## Performance Requirements

| Metric | Target |
|--------|--------|
| Feed load time (P95) | < 200ms |
| Score update latency | < 5 seconds |
| WebSocket broadcast latency | < 2 seconds |
| Memory per hub (1000 activities) | < 10MB |
| Concurrent WebSocket connections | 1000 |

---

## Example Walkthrough

### Scenario: Activity Goes Viral

```
T+0:00  - Activity "Basketball Tonight" created (score: 0.5, freshness boost)
T+0:15  - 2 people join (score: 0.55)
T+0:30  - 3 more join (score: 0.65, velocity increasing)
T+0:45  - 4 more join (score: 0.82, "🔥 Trending" badge)
T+1:00  - 6 more join, 15 total, 5 spots left (score: 0.91, "Almost full!")
T+1:15  - Activity full (score: 0.60, drops because no more joins possible)
```

Walk through the score calculation at each step.

---

## Constraints

- PostgreSQL as primary database
- Can use Redis if justified (additional infra cost)
- WebSocket already implemented (extend, don't replace)
- Must work with existing ActivityService, ParticipationService
- No external real-time databases (Firebase, etc.)

---

## Success Criteria

A good solution will:
- Feel "alive" to users (feed changes without refresh)
- Not spam users with constant updates
- Handle load spikes (popular activity gets 100 joins in 5 min)
- Degrade gracefully (if real-time fails, static still works)
- Be maintainable (not over-engineered)
