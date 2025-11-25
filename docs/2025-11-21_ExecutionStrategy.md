---
title: "Gatherly Execution Strategy — 2025-11-21"
owner: "Product & Engineering"
lastReviewed: "2025-11-21"
---

# 1. Objective

Deliver a safety-first micro-hangout MVP in ~2 weeks while laying the rails for the GATHERINGS™ recommendation roadmap. This plan combines immediate operational tasks (runbooks, QA, SMS) with ML-enabling work so that we can accelerate post-launch personalization without thrash.

# 2. Current Readiness Snapshot

- **Backend:** Production-ready (Spring Boot 3.2, PostgreSQL, Flyway, EventLogService, ReportService, InviteTokenService, MessageExpiryJob).  
- **Frontend:** 85% complete; all major screens exist, WebSocket chat & moderation integrated. Remaining work is polish (post-meet feedback modal, template shortcuts, settings entry) plus run instructions.  
- **Safety:** Invite-only gating, identity reveal logic, Slack reporting, contact hashing & opt-out, ephemeral chat.  
- **ML foundation:** `events` table, `UserPhoneHash`, telemetry scaffolding in place; no models yet.  
- **Outstanding ops:** real SMS provider, canonical runbook, end-to-end QA, post-meet feedback loop, high-level ML roadmap.

# 3. Two-Week Production-Ready Plan

| Day(s) | Focus | Key outputs |
|--------|-------|-------------|
| 1–2 | Moderation & invite QA | Harden `ReportService` → Slack pipeline, verify auto-ban thresholds, document escalation playbook. Validate invite-token flow from Feed & Detail (generate, share, redeem). |
| 3 | Contact hashing QA | Exercise `ContactsUploadScreen` on iOS + Android, confirm hashed payload ≈ backend counts, finalize privacy copy & opt-out messaging. |
| 4–6 | Frontend polish | Wire `TemplateSelectionScreen` into `CreateActivityScreen`, add post-meet feedback modal + `meetup_outcome` event, refine Settings navigation & CTA entry points. |
| 7 | Chat & reveal QA | Multi-device WebSocket + fallback tests, heading-there flow, identity reveal thresholds, message expiry countdown accuracy. |
| 8 | End-to-end testing | Scripted run: Auth → Feed → Join → Invite → Chat → Report → Contacts → Feedback. Log defects + metrics gaps. |
| 9–10 | Private alpha packaging | Hook `AuthService` to real SMS provider, configure secrets, run `mvn spring-boot:run` on staging, produce TestFlight/APK builds, seed pilot accounts. |
| 11–14 | Launch prep | Bug bash, analytics dashboards, finalize `docs/2025-11-21_Runbook.md`, update READMEs, prepare support + comms. |

Parallel threads: (a) runbook + QA scripting (Days 1–10), (b) telemetry hardening (add `view_activity`, `join_attempt`, `invite_sent`, `meetup_outcome`), (c) ops (monitoring, backups per `DATABASE_BACKUP_STRATEGY.md`).

# 4. ML Integration Roadmap

| Phase | Goal | Timeline | Actions |
|-------|------|----------|---------|
| 0. Telemetry Hardening | Reliable signals | Week 0–1 | Add missing events + metadata, document schema in `TELEMETRY_IMPLEMENTATION.md`, verify EventLogService hit rates. |
| 1. Heuristic GATHERINGS™ Score | Better feed without models | Week 2–4 | Backend scoring (time decay, category affinity, hub safety multiplier, mutuals boost, report penalty). Return `score` in `GET /activities`. |
| 2. Join Probability Model | First ML ranking | Month 2–3 | ETL from `events`, train logistic/GBDT, expose `/gatherings/recommended`, add analytics for join CTR / report rate deltas. |
| 3. Connections Layer | Social fit hints | Month 3–4 | Build compatibility features (co-attendance + positive outcomes). Surface “Great fit for you” badges, use as tie-breaker in ranking. |
| 4. Intent-driven Personalization | Real-time context | Month 4+ | Add “Tonight I feel like…” prompt, log intent, feed into ranking model + analytics. |

Guardrails: no raw PII exposure, explainable recommendations, safety constraints override ML, monitor fairness (hub/category spread).

# 5. Post-MVP Milestones

1. **Private Alpha (Week 3)** – deploy to 20–30 trusted users, measure activation, chat reliability, report response time.  
2. **Closed Beta (Weeks 4–8)** – add hubs/users, instrument `meetup_outcome`, ship heuristic scoring, gather NPS.  
3. **Public Beta Prep (Weeks 8–12)** – finalize runbooks, deploy first ML model, integrate basic analytics dashboard, plan growth experiments.  
4. **Early Scale (Quarter 2)** – connections layer, venue partnerships, push notifications, start ML experimentation cadence.

# 6. Ownership & Resourcing

| Workstream | DRI | Support |
|------------|-----|---------|
| Backend platform & safety | Lead backend engineer | Founder |
| Mobile/UI polish | Mobile lead | Product/design |
| Ops (SMS, deploy, runbook) | DevOps / infra | Founder |
| QA & launch readiness | PM/QA lead | Entire squad |
| ML & telemetry | Data/ML lead (interim: founder) | Backend |

Hiring queue after MVP: 1) dedicated mobile engineer, 2) ML/data engineer, 3) community/ops lead.

# 7. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| SMS provider delay | Blocks onboarding | Start Twilio/MSG91 integration Day 1, keep mock path for internal tests. |
| WebSocket instability | Chat degradation | Leverage `useWebSocket` auto-reconnect + 3s polling fallback, add logging + alerting. |
| Missing feedback data | ML cannot progress | Ship post-meet modal + reminder pings, store `meetup_outcome` events. |
| Ops gaps | Launch chaos | Complete `docs/2025-11-21_Runbook.md`, run tabletop exercise before alpha. |
| Safety regression | Trust damage | Keep Slack alerts live, define responder rotation, implement auto-disable for repeat offenders. |

---

This plan should be revisited after the private alpha or whenever strategy shifts.
