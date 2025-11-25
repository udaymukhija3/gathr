# Gathr – 2‑Week Private Alpha Sprint Plan
**Version:** 0.1  
**Date:** 2025‑11‑21  
**Goal:** Run a focused 2‑week sprint to get Gathr to a *live* private alpha in one hub (e.g. Cyberhub) with 5–10 real users completing at least 3 actual meetups.

---

## Guiding Principles

- **Ship daily:** Every day ends with something a real user could touch.
- **Bias to reality:** Prefer small, end‑to‑end tests over more planning.
- **Safety & trust first:** No feature ships that weakens safety controls.
- **Heuristics before ML:** Use simple rules + explicit preferences now; collect data for ML later.

---

## Week 1 – Make the Loop Real (Days 1–7)

### Day 1–2 – Real SMS + Happy-Path Auth

**Backend**

- [ ] Configure SMS provider (e.g. Twilio/MSG91):
  - [ ] Add provider SDK dependency in `pom.xml`
  - [ ] Implement `TwilioOtpServiceImpl` (or equivalent) and wire into `OtpService` / `AuthService`
  - [ ] Read credentials from env (`SMS_PROVIDER`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, etc.)
  - [ ] Add minimal logging + error handling around SMS failures

**Frontend**

- [ ] Update `PhoneEntryScreen` + `OtpVerifyScreen`:
  - [ ] Display clear errors for 4xx/5xx from `/auth/otp/start` and `/auth/otp/verify`
  - [ ] Handle 429 (rate limit) gracefully (“Too many requests, try again in X minutes”)

**Smoke Test**

- [ ] From a physical device (Expo), run: Phone → OTP → Feed end‑to‑end using real SMS

---

### Day 3 – Post-Meet Feedback Loop

**Backend**

- [ ] Add a `meetup_outcome` event via `EventLogService` with fields:
  - `userId`, `activityId`, `didMeet` (bool), `wouldMeetAgain` (bool), `ts`
- [ ] (Optional) Add `activity_feedback` table with basic rating + free‑text

**Frontend**

- [ ] Create `PostMeetFeedbackModal` (new screen or component):
  - Fields: “Did you meet?” (Yes/No), “Would you meet them again?” (Yes/No), optional comment
  - Trigger: when user opens an activity or chat where `endTime` < now and no prior feedback
- [ ] Call a new endpoint (e.g. `POST /activities/:id/feedback` or `/events/meetup_outcome`)

**Test**

- [ ] Manually fast‑forward an activity’s `endTime` and verify the modal appears and logs an event

---

### Day 4 – Preference Capture (Vibes & Interests)

**Backend**

- [ ] Add `user_preferences` support:
  - [ ] Either a `user_preferences` table (user_id, type, value, created_at) or a JSONB column on `users`
  - [ ] Implement `GET /me/profile` and `POST /me/profile` (with validation, auth)
- [ ] Log `profile_set` event via `EventLogService` (include selected interests/intents)

**Frontend**

- [ ] Add a simple “Set your vibe” flow after OTP success:
  - [ ] Screen to pick interests (e.g. Coffee, Sports, Art, Music, Games)
  - [ ] Optional “Tonight I feel like…” quick toggle
- [ ] Call `POST /me/profile` and store result in `UserContext`

**Test**

- [ ] Go through onboarding, set preferences, reload app, and verify preferences persist and are returned by `GET /me/profile`

---

### Day 5 – Heuristic Recommendations in Feed

**Backend**

- [ ] Extend `ActivityDto` with:
  - [ ] `score: number`
  - [ ] `scoreReason: string` (short human‑readable explanation)
- [ ] In `ActivityService.getActivitiesByHub(...)`, compute a `score` per activity:
  - Example factors:
    - +X for matching user’s selected categories
    - +Y for same hub as user’s preference
    - +Z for `mutualsCount > 0`
    - − for full/near‑full rooms, recent reports, very late/early start times
- [ ] Populate `score` and `scoreReason` in the DTO

**Frontend**

- [ ] In `FeedScreen`:
  - [ ] Sort activities by `score` descending (fallback to time if score missing)
  - [ ] Display `scoreReason` as a small subtitle on `ActivityCard` (e.g. “Because you like Coffee in Galleria”)

**Test**

- [ ] Use two different profiles (e.g. Coffee‑lover vs Sports‑lover)
- [ ] Verify feed ordering and `scoreReason` change based on profile

---

### Day 6–7 – Safety & Runbook Polish

**Safety**

- [ ] Re‑review identity reveal condition in `ActivityService.joinActivity` (`≥3 confirmed` vs `≥3 interested`) and decide if you want to adjust
- [ ] Ensure `reportsApi.create` is wired from `ChatScreen` + `ReportModal`
- [ ] Add a minimal “Block user” stub (even if it just logs for now), and an `user_blocked` event for future work

**Runbook & Ops**

- [ ] Walk through `docs/2025-11-21_Runbook.md` from scratch on a clean machine:
  - [ ] Confirm all commands and env vars work
  - [ ] Add any missing steps (e.g., `npm install`, `expo start`, `npx expo prebuild` if needed)
- [ ] Ensure CI pipeline (`.github/workflows`) runs tests on PRs
- [ ] Verify `/actuator/health` is documented in the backend README and works in your deployed env

---

## Week 2 – Real Users & Tightening (Days 8–14)

### Day 8–9 – Micro Alpha (5–10 Real Users)

**Preparation**

- [ ] Choose one hub (e.g. Cyberhub) and 2–3 time slots (e.g. Lunch, Coffee, Drinks)
- [ ] Recruit 5–10 trusted friends/colleagues
- [ ] Prepare a very short onboarding guide (how to install the app, what to expect, how to give feedback)

**Execution**

- [ ] Run at least 2–3 real Gathr meetups:
  - [ ] Create activities in the app
  - [ ] Have participants join via OTP + invite tokens where relevant
  - [ ] Use chat, test “Heading there” and identity reveal
  - [ ] After each meetup, ensure `PostMeetFeedbackModal` appears and feedback is collected

**Capture**

- [ ] Log:
  - # invited vs joined vs showed up
  - Qualitative feedback (quotes, pain points)
  - Any major bugs or confusing steps

---

### Day 10–11 – Fixes from the Field

Based on alpha observations:

- [ ] Fix **blockers**:
  - Crashes, stuck flows, broken navigation, invite/join failures
- [ ] Improve **copy**:
  - Clarify invite-only behavior, reveal rules, and safety expectations
- [ ] Adjust **heuristics**:
  - Tweak scoring weights and `scoreReason` text based on what users found confusing or misaligned

---

### Day 12–14 – Decide & Prepare for Wider Alpha

- [ ] Review alpha metrics:
  - # signups
  - # join attempts / joins per activity
  - # actual meetups and show‑up rate
  - “Would meet again” responses and any red‑flag feedback
- [ ] Decide:
  - Are we ready to expand to 20–30 users in the same hub?
  - What must be fixed before inviting more people?
- [ ] Update docs:
  - [ ] Reflect learnings in `docs/2025-11-21_VisionStatus.md`
  - [ ] Revise `docs/2025-11-21_ExecutionStrategy.md` for the next 4–6 weeks
  - [ ] Update `docs/2025-11-21_Runbook.md` with anything you learned

---

## Daily Standup Template

```markdown
### [Date]

**Yesterday**
- [shipped]
- [user feedback]
- [issues found]

**Today**
- Morning: [concrete task]
- Afternoon: [concrete task]
- End-of-day success metric: [binary outcome]

**Blocked By**
- [if any]
```

---


