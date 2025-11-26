# First 12 Concrete Issues

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
