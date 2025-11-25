---
title: "Gatherly Vision Status — 2025-11-21"
owner: "Product & Engineering"
lastReviewed: "2025-11-21"
---

# 1. Purpose

This document captures the current state of the Gatherly product against the original micro‑hangout vision (“small, safety-first gatherings in Gurgaon”) and clarifies the role of ML in the MVP. It ties directly to concrete code so that every assertion can be verified.

# 2. End-to-End Experience Coverage

The vision loop was “**Open → See Meetups → Join/Host → Chat → Show Up → Repeat**.”  
Below is how the current codebase satisfies each step.

| Step | Backend Reality | Frontend Reality | Notes |
|------|-----------------|------------------|-------|
| **Open** | `AuthController` + `AuthService` handle `POST /auth/otp/start` (with per-phone rate limiting) and `POST /auth/otp/verify` (JWT issuance). | `PhoneEntryScreen` (OTP start) → `OtpVerifyScreen` (entry & optional display name) → `UserContext` persists token in `expo-secure-store` and routes to the feed. | Mock OTP (“123456”) today; Twilio/TBD provider planned per `SMS_TEMPLATE`. |
| **See Meetups** | `ActivityService#getActivitiesByHub` pulls the day’s activities, annotating `peopleCount`, `mutualsCount`, `isInviteOnly`, `revealIdentities`, `maxMembers`. Hubs come from `HubController`. | `FeedScreen` with `HubSelector` drives `ActivityCard`s (title, times, category badge, members & mutuals). | `frontend/README.md` already advertises these fields. |
| **Join / Host** | `ActivityService#createActivity` enforces hub/category/time; `ActivityService#joinActivity` validates invite tokens, capacity, and toggles `revealIdentities` once ≥3 interested/confirmed. `InviteTokenService` issues & tracks tokens. | `CreateActivityScreen` (full form + templates work pending), `ActivityDetailScreen` + `InviteModal`, `FeedScreen` join CTA. Client checks `maxMembers` and falls back to modal if invite-only. | Join path logs `activity_joined` via `EventLogService` for downstream analytics. |
| **Group Chat** | `MessageService` + `/activities/{id}/messages`, WebSocket broker (`WebSocketConfig`). `MessageExpiryJob` prunes messages >24h after `activity.end_time`. `ReportService` writes `report_created` events and pings Slack via `WebClient`. | `ChatScreen` uses `useWebSocket` (with 3s polling fallback), shows connection status + message expiry countdown, offers “Heading There” (maps to `POST /activities/{id}/confirm`), and `ReportModal` from each `ChatBubble`. | Further safety: `ReportService` also writes to `events` table so we can automate bans later. |
| **Show Up** | `activities/:id/confirm` marks `Participation` as `CONFIRMED`, feeding the reveal logic. Future: add `meetup_outcome` events to `EventLogService`. | “Heading There” CTA in chat; once confirm threshold reached the UI flips to reveal names/avatars (using `activity.revealIdentities`). | Post-meet feedback modal still pending (ties into ML plan). |
| **Repeat** | `EventLogService` captures `activity_created`, `activity_joined`, `activity_confirmed`, `message_sent`, `report_created`, `invite_token_generated`. Contact hashing (`UserPhoneHash`) provides mutual counts. | `FeedScreen` + `ActivityDetailScreen` surface mutuals and invite gating, encouraging trust loops. `SettingsScreen` exposes contact sharing toggle + logout. | These signals are the backbone for future ML-based GATHERINGS™ ranking & “connections.” |

# 3. Is ML part of the MVP?

**Short answer:** ML is **not a blocking requirement** for the first release, but the MVP already emits the telemetry needed for GATHERINGS™ ranking and “connections” models.

- The launch candidate relies on deterministic rules (time, hub, invite-only, mutual counts, report gating) to validate the core experience.
- `EventLogService` and `UserPhoneHash` ensure every join/confirm/report/invite carries metadata, so we can train models without reworking the product.
- ML becomes mandatory when we scale beyond curated hubs and need personalization, room viability prediction, and automated moderation. For MVP the priority remains shipping a safe, delightful loop.

# 4. MVP Readiness Snapshot (2025-11-21)

| Dimension | Status | Evidence / Notes |
|-----------|--------|------------------|
| Backend platform | ✅ Production-ready | Spring Boot 3.2 + PostgreSQL + Flyway; controllers/services for auth, hubs, activities, invites, chat, moderation. |
| Frontend experience | 🟡 ~85% | All major screens exist; polish items remain (post-meet feedback modal, template shortcuts, settings UX) per `frontend/NEXT_STEPS.md`. |
| Safety & privacy | ✅ | Invite-only enforcement, reveal logic, `ReportService`+Slack, contact hashing opt-out, message expiry job, Settings controls. |
| Operations & runbooks | 🟡 | Documentation scattered across READMEs; no single “spin up & verify” guide (addressed in new runbook). |
| ML readiness | 🟡 foundation only | `events` table + telemetry scaffolding exist; no ranking/compatibility models yet. |
| Go-to-market | 🟡 | Needs real SMS provider, end-to-end QA, analytics hooks, and runbook before private alpha. |

**Estimated effort to MVP:** ≈2 focused weeks following the production-ready checklist (WebSocket QA, invite flow QA, SMS integration, post-meet feedback, runbook).

# 5. Open Gaps to Track

1. **Post-meet feedback loop** – no UX or `meetup_outcome` events yet; required for trust + ML labeling.
2. **Activity templates integration** – `TemplateSelectionScreen` exists but is not wired into `CreateActivityScreen`.
3. **SMS provider** – `AuthService` still uses mock OTP; Twilio/MSG91 wiring needed for real-world pilots.
4. **Runbooks & QA** – need canonical instructions for backend/frontend start-up and verification.
5. **ML execution plan** – strategy exists in prose; requires committed roadmap to move from telemetry to heuristics/models.

# 6. Source References

- `VISION_ASSESSMENT.md` – overall 90% completion & safety emphasis.
- `frontend/NEXT_STEPS.md` – integration tasks & production-ready path.
- `PROGRESS_AND_ROADMAP.md` – pre-launch checklist + deferred (V2/ML) items.
- `STRATEGIC_ANALYSIS.md` – vision narrative & ML role.
- `README.md`, `frontend/README.md` – current feature surface & run instructions.

---

This document should be revisited after the private alpha milestone or whenever product scope changes materially.
