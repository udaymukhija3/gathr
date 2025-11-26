# Project Audit & Execution Plan

## 1. Executive Priority List

1.  **Implement Block & Report System (Safety Critical)**
    *   **Rationale:** Non-negotiable for public alpha; users must be able to block creeps and report unsafe behavior immediately.
    *   **Acceptance Criteria:** User can block another user (invisible to each other); User can report activity/user with reason; Admin can view reports.
    *   **Complexity:** Medium

2.  **Enforce Identity Reveal Thresholds**
    *   **Rationale:** Core value prop ("safe until committed"); prevents stalking and lowers barrier for women to join.
    *   **Acceptance Criteria:** Names/Photos hidden on activity detail until user joins AND activity has 3+ interested/confirmed participants.
    *   **Complexity:** Medium

3.  **Deploy "Tonight" Feed Logic & Ranking**
    *   **Rationale:** The "spontaneity" hook requires showing relevant, immediate activities, not stale ones.
    *   **Acceptance Criteria:** `GET /plans` returns only future activities (0-24h window), sorted by time + hub proximity.
    *   **Complexity:** Small

4.  **Integrate Twilio/MSG91 for Real OTP**
    *   **Rationale:** Current mock OTP allows anyone to login as anyone; essential for identity verification and safety.
    *   **Acceptance Criteria:** User receives actual SMS code; backend verifies code with provider; mock disabled in prod.
    *   **Complexity:** Small

5.  **Implement Mutual Contacts "Hash" Sync**
    *   **Rationale:** "3 Mutuals" is the strongest trust signal; needs privacy-preserving implementation (hashing).
    *   **Acceptance Criteria:** App uploads hashed contacts; Backend stores hashes; `GET /plans` returns "X mutuals" count for participants.
    *   **Complexity:** Large

6.  **Ship Minimal "Hub" Home Screen**
    *   **Rationale:** Focus users on the 3 core hubs (Cyberhub, etc.) to build density, rather than a generic map.
    *   **Acceptance Criteria:** Home screen defaults to user's selected hub; Filter toggle for "My Hub" vs "All Gurgaon".
    *   **Complexity:** Small

---

## 2. First 12 Concrete Issues

1.  **[MVP/Core] Replace Mock OTP with Twilio Service**
    *   **Body:** Integrate Twilio SDK. Update `AuthService` to send real SMS. Add `twilio.enabled` flag in config.
    *   **Acceptance:** Real SMS received on login.
    *   **Size:** Small

2.  **[MVP/Safety] Create `blocks` and `reports` Tables**
    *   **Body:** Add Flyway migration for `blocks` (blocker_id, blocked_id) and `reports` (reporter_id, reported_id, reason, status).
    *   **Acceptance:** Tables exist in DB.
    *   **Size:** Small

3.  **[MVP/Safety] Implement Block User API**
    *   **Body:** `POST /users/{id}/block`. Service layer: check if already blocked, save to DB.
    *   **Acceptance:** API returns 200 OK. Blocked user cannot be seen in feed/search.
    *   **Size:** Small

4.  **[MVP/Safety] Filter Blocked Users from Feed & Chat**
    *   **Body:** Update `ActivityService` and `ChatService` to exclude content from blocked/blocking users.
    *   **Acceptance:** User A blocks B -> A sees nothing from B, B sees nothing from A.
    *   **Size:** Medium

5.  **[MVP/Core] Implement "Identity Reveal" Logic in Activity DTO**
    *   **Body:** In `ActivityController.getById`, check `participants.size() >= 3`. If false, mask names/avatars of participants (unless `user` is already joined).
    *   **Acceptance:** Non-joined user sees "Member #1" instead of "Rahul".
    *   **Size:** Medium

6.  **[MVP/Infra] Add "Hub" Filter to `GET /activities`**
    *   **Body:** Ensure `hubId` param is mandatory or defaults to user's home hub. Filter query by `hub_id`.
    *   **Acceptance:** Feed only shows activities in selected hub.
    *   **Size:** Small

7.  **[MVP/ML] Implement Contact Hash Upload API**
    *   **Body:** `POST /contacts/upload`. Receives list of SHA-256 hashed phone numbers. Stores in `user_contact_hashes` table.
    *   **Acceptance:** 1000 contacts uploaded in < 1s.
    *   **Size:** Medium

8.  **[MVP/ML] Calculate & Return Mutual Count**
    *   **Body:** In `ActivityDto`, for each participant, calculate intersection of viewer's contacts and participant's contacts. Return `mutual_count`.
    *   **Acceptance:** JSON response includes `mutual_count: 5`.
    *   **Size:** Large

9.  **[MVP/Core] Enforce 24h Activity Window**
    *   **Body:** Update `ActivityRepository` query to `WHERE start_time BETWEEN NOW() AND NOW() + INTERVAL '24 HOURS'`.
    *   **Acceptance:** Activities next week do not show up.
    *   **Size:** Small

10. **[MVP/Safety] Add "Report Activity" Flow**
    *   **Body:** `POST /activities/{id}/report`. Reason enum: SPAM, DANGEROUS, HARASSMENT.
    *   **Acceptance:** Report saved. Admin notified (log/slack webhook).
    *   **Size:** Small

11. **[Infra] Dockerize Backend for Local Dev**
    *   **Body:** Create `Dockerfile` and `docker-compose.yml` with Postgres + Spring Boot app.
    *   **Acceptance:** `docker-compose up` starts everything.
    *   **Size:** Small

12. **[MVP/Core] Add "Join Request" Logic for Invite-Only**
    *   **Body:** If activity is `invite_only`, `POST /join` creates a REQUEST instead of PARTICIPATION. Creator must approve.
    *   **Acceptance:** User status is PENDING until approved.
    *   **Size:** Medium

---

## 3. First 10 Commits

1.  `db/migration/V20__create_blocks_reports.sql`: Add DDL for blocks and reports tables.
2.  `src/main/java/com/gathr/entity/Block.java`: Add Block entity class.
3.  `src/main/java/com/gathr/repository/BlockRepository.java`: Add repository interface.
4.  `src/main/java/com/gathr/service/BlockService.java`: Add blockUser and isBlocked methods.
5.  `src/main/java/com/gathr/controller/UserController.java`: Add `POST /{id}/block` endpoint.
6.  `src/main/java/com/gathr/service/ActivityService.java`: Filter blocked users in `getActivities`.
7.  `src/main/java/com/gathr/dto/ActivityDto.java`: Add `masked` boolean and logic to mask participant data.
8.  `src/main/java/com/gathr/controller/ActivityController.java`: Apply masking logic in `getById`.
9.  `src/main/java/com/gathr/repository/ActivityRepository.java`: Add `findUpcoming(Pageable page)` with 24h filter.
10. `src/main/java/com/gathr/controller/FeedController.java`: Wire up `findUpcoming` to main feed endpoint.

---

## 4. API Contract Skeleton

### Auth
*   **POST /auth/otp/start**
    *   Request: `{ "phoneNumber": "+919876543210" }`
    *   Response: `{ "message": "OTP sent" }`
*   **POST /auth/otp/verify**
    *   Request: `{ "phoneNumber": "+919876543210", "otp": "123456", "deviceId": "uuid" }`
    *   Response: `{ "token": "jwt...", "user": { "id": 1, "isNew": false } }`

### Plans (Activities)
*   **GET /plans?lat=28.45&lon=77.02&hubId=1**
    *   Response:
        ```json
        [
          {
            "id": 101,
            "title": "Badminton Doubles",
            "startTime": "2025-11-26T19:00:00Z",
            "hub": "Cyberhub",
            "participants": [
              { "id": 5, "name": "Member #1", "avatar": null, "isMasked": true, "mutualCount": 2 }
            ],
            "status": "OPEN"
          }
        ]
        ```
*   **POST /plans**
    *   Request:
        ```json
        {
          "title": "Coffee & Code",
          "activityType": "WORK",
          "startTime": "2025-11-26T18:00:00Z",
          "hubId": 1,
          "maxParticipants": 4,
          "description": "Casual chat"
        }
        ```
*   **GET /plans/:id**
    *   Response: Full details. If `participants.length < 3` and `!currentUser.joined`, mask names.
*   **POST /plans/:id/join**
    *   Request: `{ "status": "INTERESTED" }` (or CONFIRMED)

### Contacts
*   **POST /contacts/upload**
    *   Request: `{ "hashes": ["sha256_hash_1", "sha256_hash_2"] }`
    *   Response: `{ "processed": 500 }`

### WebSocket
*   **Topic:** `/topic/plans/{planId}`
*   **Message:**
    ```json
    {
      "type": "CHAT",
      "senderId": 12,
      "content": "I'm here!",
      "timestamp": "..."
    }
    ```

---

## 5. Minimal DB DDL

```sql
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
```

---

## 6. ML Instrumentation Plan

**Storage:** Write to `event_logs` table in Postgres (for MVP) -> Export to CSV/Parquet for training.

**Events:**
1.  `VIEW_FEED`: `{ "hub_id": 1, "lat": ..., "lon": ... }` (Backend)
2.  `VIEW_ACTIVITY_DETAIL`: `{ "activity_id": 101, "source": "FEED" }` (Backend)
3.  `JOIN_ATTEMPT`: `{ "activity_id": 101, "status": "INTERESTED" }` (Backend)
4.  `JOIN_SUCCESS`: `{ "activity_id": 101 }` (Backend)
5.  `SEND_MESSAGE`: `{ "activity_id": 101, "length": 45 }` (Backend)
6.  `UPLOAD_CONTACTS`: `{ "count": 150 }` (Backend)
7.  `BLOCK_USER`: `{ "target_id": 55, "context": "CHAT" }` (Backend)
8.  `APP_OPEN`: `{ "device_id": "...", "time": "..." }` (Frontend/API)

---

## 7. Safety + Moderation Checklist

*   **Onboarding Copy:** "Gathr is a community for real friends. We have zero tolerance for harassment. Be kind, be safe."
*   **Report Flow:**
    *   User taps "Report" -> Selects Reason (Harassment, Spam, Fake) -> Adds optional comment -> Submit.
    *   **Action:** Hide content immediately locally. Flag for admin.
*   **Auto-Action Rules:**
    *   **2 Reports (Unique Reporters):** Auto-suspend user for 24h.
    *   **5 Reports:** Permanent ban (requires admin review to reverse).
*   **Data Retention:** Keep chat logs for 30 days for safety audits, then hard delete. Keep report metadata forever.

---

## 8. Dev DX & Local Dev Runbook

**`docker-compose.yml` snippet:**
```yaml
version: '3.8'
services:
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: gathr
      POSTGRES_USER: gathr
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/gathr
      SPRING_DATASOURCE_USERNAME: gathr
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      - db
```

**Developer Checklist:**
1.  Install Docker & Java 17.
2.  Run `docker-compose up -d db`.
3.  Run `./mvnw spring-boot:run`.
4.  (Optional) Run `./scripts/seed_data.sh` to populate 3 hubs and 10 dummy activities.

---

## 9. Risk List & What to Defer

1.  **Complex Payment Flows:** DEFER. Use "pay at venue" or splitwise. Integration is a distraction.
2.  **Internationalization:** DEFER. Gurgaon only. English only.
3.  **Partner Automation:** DEFER. Manually onboard venues if needed. No "Partner Portal".
4.  **Fancy ML Models:** DEFER. Use simple heuristics (time * distance) until >1000 users.
5.  **Video/Voice Chat:** DEFER. High bandwidth/cost/complexity. Text is enough.
6.  **Gamification (Badges/Levels):** DEFER. Core loop (meeting people) must work first.
7.  **Social Graph Visualization:** DEFER. Cool but useless for MVP.
8.  **Public User Profiles:** DEFER. Privacy risk. Only show profiles in context of an activity.

---

## 10. Suggested A/B Test & ML KPI

*   **Experiment:** **"Blind" vs "Open" Feed.**
    *   **Variant A:** Show participant photos immediately.
    *   **Variant B:** Show "3 participants" (avatars blurred) until click.
    *   **Hypothesis:** Variant B increases click-through rate (curiosity) and reduces bias against empty-looking groups.
*   **KPI:** **Join Rate per Impression.** (Joins / Feed Views).
    *   **Guardrail:** Report rate (ensure clickbait doesn't lead to bad experiences).

---

## 11. Deliverables Folder

Create a folder `deliverables/` with the following files:

1.  `deliverables/ISSUES.md`: The 12 concrete issues from Section 2.
2.  `deliverables/PR_TEMPLATE.md`: A simple PR template enforcing "Test Plan" and "Safety Check".
3.  `deliverables/PLAN_SCHEMA.sql`: The SQL from Section 5.
4.  `deliverables/EVENT_SCHEMAS.json`: The JSON schemas from Section 6.
5.  `deliverables/SAFETY_CHECKLIST.md`: The content from Section 7.
