# Opus Prompt: Fraud & Abuse Detection System

## Context

Building **Gathr** - a spontaneous activity discovery app where strangers meet in person.

**Safety is Critical:** Unlike purely online platforms, bad actors on Gathr can cause real-world harm. We need to detect and prevent:
- Fake accounts
- Spam/promotional activities
- Coordinated manipulation
- Harassment patterns
- Scams targeting users

**Constraint:** No ML models available. Must use rule-based heuristics that can be tuned.

**Stack:** Spring Boot 3 (Java 17) + PostgreSQL
**Scale:** 100K users

---

## Threat Model

### 1. Fake Account Creation
**Goal:** Create multiple accounts to spam, manipulate, or evade bans
**Signals:**
- Multiple signups from same IP/device
- Burst signups (many accounts in short time)
- Phone numbers from known VoIP providers
- No contacts uploaded (real users have contacts)
- Identical or similar names/bios

### 2. Activity Spam
**Goal:** Promote businesses, events, or scams through fake activities
**Signals:**
- Same user creating many similar activities
- Activities with promotional language (URLs, "DM me", prices)
- Activities in unusual hours or patterns
- High creation rate, low participation
- Copy-paste descriptions

### 3. Coordinated Manipulation
**Goal:** Artificially boost activity visibility or user trust scores
**Signals:**
- Same users always joining each other's activities
- Circular rating (A rates B, B rates A, repeatedly)
- New accounts joining then immediately leaving
- Suspicious timing patterns (joins happen in bursts)

### 4. Harassment Patterns
**Goal:** Target specific users across multiple activities
**Signals:**
- Same user reported by multiple different people
- User follows another across activities
- Negative interactions after leaving activity
- Creating activities targeting specific users

### 5. Scam Patterns
**Goal:** Extract money or personal info from users
**Signals:**
- Activities that redirect to external sites
- Requests for payment outside platform
- Activities with too-good-to-be-true promises
- Private messaging patterns (moving convo off-platform)

---

## Current Data Available

### Users
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20),
    name VARCHAR(100),
    created_at TIMESTAMP,
    trust_score INTEGER,
    last_login_at TIMESTAMP
);

-- Login tracking
CREATE TABLE user_sessions (
    user_id BIGINT,
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    user_agent TEXT,
    created_at TIMESTAMP
);
```

### Activities
```sql
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200),
    description TEXT,
    created_by BIGINT,
    hub_id BIGINT,
    category VARCHAR(50),
    start_time TIMESTAMP,
    created_at TIMESTAMP,
    status VARCHAR(20)
);
```

### Participations
```sql
CREATE TABLE participations (
    user_id BIGINT,
    activity_id BIGINT,
    status VARCHAR(20),
    joined_at TIMESTAMP,
    left_at TIMESTAMP
);
```

### Reports
```sql
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT,
    target_user_id BIGINT,
    activity_id BIGINT,
    reason TEXT,
    category VARCHAR(50), -- HARASSMENT, SPAM, SCAM, INAPPROPRIATE, OTHER
    status VARCHAR(20), -- PENDING, REVIEWED, ACTIONED, DISMISSED
    created_at TIMESTAMP
);
```

### Messages
```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT,
    user_id BIGINT,
    text TEXT,
    created_at TIMESTAMP
);
```

---

## Task

Design a **rule-based fraud and abuse detection system** that:

1. **Detects suspicious patterns** in real-time or near-real-time
2. **Scores risk** for users and activities
3. **Takes automated actions** (flag, restrict, ban)
4. **Supports manual review** with evidence
5. **Allows appeals** for false positives
6. **Learns from decisions** (tune thresholds based on outcomes)

---

## Detection Rules to Design

### Category 1: Account Risk Scoring

Design rules to score new account risk (0-100):

| Rule | Risk Score | Condition |
|------|-----------|-----------|
| VoIP phone number | +30 | Phone prefix in known VoIP list |
| Multiple accounts same IP | +25 | >1 account from IP in 24hrs |
| No contacts uploaded (after 7 days) | +15 | contact_count = 0 |
| Generic name pattern | +10 | "User12345", single letter names |
| Rapid activity after signup | +20 | Created activity within 10 min of signup |
| ... | ... | ... |

**Actions:**
- Score > 70: Auto-block, require manual review
- Score 50-70: Shadow-restrict (activities hidden from feed)
- Score 30-50: Flag for monitoring

### Category 2: Activity Spam Detection

Design rules to flag spam activities:

| Rule | Confidence | Condition |
|------|-----------|-----------|
| URL in description | High | Contains http/https links |
| Price mention | Medium | Contains ₹, Rs, "payment", "fee" |
| Duplicate content | High | Description matches another activity (fuzzy) |
| High creation rate | High | User created >3 activities in 24hrs |
| External contact request | High | "WhatsApp", "DM", "call me" in description |
| Unusual timing | Medium | Activity at 3 AM, or 2 months in future |
| ... | ... | ... |

**Actions:**
- High confidence spam: Auto-remove, notify user
- Medium confidence: Hide from feed, manual review
- Repeated spam: Account restriction

### Category 3: Coordination Detection

Design rules to detect coordinated behavior:

| Rule | Condition |
|------|-----------|
| Ring detection | A→B→C→A rating pattern |
| Always together | Same 3+ users in >5 activities together |
| Burst joining | >5 users join same activity within 60 seconds |
| Creator-participant loop | User A creates, B joins; B creates, A joins (>3 times) |
| New account clusters | Multiple new accounts (<7 days) in same activity |

### Category 4: Harassment Detection

Design rules to detect harassment:

| Rule | Condition |
|------|-----------|
| Multi-report target | User reported by >2 different users in 7 days |
| Following pattern | User joins 3+ activities that same person joined |
| Post-leave negativity | Message tone changes after someone leaves |
| Activity targeting | Activity description mentions specific user |
| Repeated blocking | User blocked by >3 people |

### Category 5: Message Content Analysis

Design keyword/pattern rules for messages:

| Pattern | Category | Action |
|---------|----------|--------|
| Phone number patterns | Info extraction | Flag |
| Payment app names | Scam risk | Flag |
| Aggressive language | Harassment | Flag |
| External links | Spam/Scam | Block link, flag |
| Repeated identical messages | Spam | Rate limit |

---

## System Architecture

### 1. Event-Driven Detection
```
User Action → Event Bus → Detection Rules → Risk Score Update → Action
```

### 2. Components Needed

**Risk Score Service**
- Maintains risk scores for users and activities
- Updates on each relevant event
- Provides risk assessment API

**Rule Engine**
- Configurable rules (stored in DB, not hardcoded)
- Each rule: condition, risk_delta, action
- Rules can be enabled/disabled, thresholds tuned

**Action Service**
- Executes automated actions (flag, restrict, ban)
- Logs all actions for audit
- Supports manual override

**Review Queue**
- Dashboard for moderators
- Shows flagged items with evidence
- Supports bulk actions

**Appeal System**
- Users can contest actions
- Appeal reviewed by human
- Decision logged and fed back

---

## Expected Output

### 1. Risk Scoring Model
```java
class UserRiskScore {
    Long userId;
    int baseScore;           // From signup signals
    int behaviorScore;       // From actions over time
    int reportScore;         // From reports received
    int coordinationScore;   // From network analysis
    int totalScore;          // Weighted combination
    List<RiskFactor> factors; // Explainable factors
}
```

### 2. Rule Schema
```sql
CREATE TABLE detection_rules (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    category VARCHAR(50),    -- FAKE_ACCOUNT, SPAM, COORDINATION, HARASSMENT
    condition_type VARCHAR(50),
    condition_params JSONB,  -- {"threshold": 3, "timeWindow": "24h"}
    risk_delta INTEGER,      -- How much to add to risk score
    action VARCHAR(50),      -- FLAG, RESTRICT, BAN, NONE
    enabled BOOLEAN,
    created_at TIMESTAMP
);

-- Example rules
INSERT INTO detection_rules VALUES
(1, 'voip_phone', 'FAKE_ACCOUNT', 'PHONE_PREFIX_MATCH',
 '{"prefixes": ["+1800", "+1888"]}', 30, 'FLAG', true, NOW()),
(2, 'multi_account_ip', 'FAKE_ACCOUNT', 'IP_COUNT_THRESHOLD',
 '{"count": 2, "window": "24h"}', 25, 'FLAG', true, NOW());
```

### 3. Detection Algorithms

For each category, provide:
- Pseudocode for detection logic
- SQL queries needed
- Performance considerations
- False positive mitigation

### 4. Action Matrix
| Risk Score | User Status | Restrictions |
|------------|-------------|--------------|
| 0-30 | Normal | None |
| 31-50 | Monitored | Activities manually reviewed |
| 51-70 | Restricted | Can't create activities, can only join |
| 71-90 | Suspended | Can't interact, pending review |
| 91-100 | Banned | Account disabled |

### 5. Event Triggers
```java
// Events that trigger detection
@EventListener
public void onUserCreated(UserCreatedEvent e) { ... }

@EventListener
public void onActivityCreated(ActivityCreatedEvent e) { ... }

@EventListener
public void onParticipationChanged(ParticipationEvent e) { ... }

@EventListener
public void onMessageSent(MessageEvent e) { ... }

@EventListener
public void onReportFiled(ReportEvent e) { ... }
```

### 6. Batch Detection Jobs
For patterns that need historical analysis:
- Coordination detection (runs hourly)
- Account clustering (runs daily)
- Trend analysis (runs daily)

### 7. Moderator Dashboard Requirements
- Queue of flagged items sorted by risk
- Evidence summary for each item
- One-click actions: Approve, Restrict, Ban, Dismiss
- Appeal queue with user's statement
- Metrics: False positive rate, review time

### 8. Appeal Process
```
User banned → User submits appeal (reason, evidence)
→ Appeal queued → Moderator reviews
→ Decision: Uphold / Overturn / Reduce
→ User notified → Action logged
```

### 9. Tuning Mechanism
- Track outcomes of automated decisions
- Calculate false positive rate per rule
- Suggest threshold adjustments
- A/B test rule changes

### 10. Privacy Considerations
- What data to log for detection?
- Retention period for risk signals
- What to show user when restricted?
- GDPR/data deletion handling

---

## Example Scenarios

Walk through detection for:

### Scenario 1: Fake Account Farm
10 accounts created from same IP within 1 hour, all with similar names, no contacts.

### Scenario 2: Business Promotion
User creates activity "Free Yoga Class" with description containing business website and pricing.

### Scenario 3: Rating Ring
Users A, B, C consistently rate each other 5 stars after activities they create.

### Scenario 4: Harassment Campaign
User X reports User Y. Next day, 2 more users report User Y for similar reasons.

### Scenario 5: False Positive
Legitimate user creates 4 activities for a weekend trip (all related, not spam).

---

## Constraints

- No external ML services
- No third-party fraud detection APIs (cost constraint)
- Must be explainable (can tell user why they were restricted)
- Must have appeal path (can't be fully automated ban)
- PostgreSQL only (no specialized databases)

---

## Success Criteria

A good solution will:
- Catch >80% of obvious abuse (spam, fake accounts)
- Have <10% false positive rate on restrictions
- Process detection in <1 second per event
- Be tunable without code changes
- Provide clear evidence for human reviewers
- Scale to 100K users, 10K events/day
