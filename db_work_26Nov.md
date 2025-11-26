Key gaps:
Mutual-contact hashing spec is implied but not concrete — ISSUES.md and event schemas call for hashed contacts, but there’s no explicit client-side hashing pseudocode, salt policy, or hash matching SQL. Add a short spec and code sample (client-side SHA256 with per-device salt + server matching) as an immediate doc.  ￼  ￼
	3.	Event logging integration points not yet placed in code — you have great event schema file but not the exact backend insertion points. We need a tiny event_log table + middleware to write events for the 8 schemas; add this before training data collection.  ￼
	4.	No explicit CI / tests wired to issues — PR template is excellent, but add a minimal CI pipeline (run unit tests, linters, DB migrations) and one integration test path (create plan → join → chat).  ￼
	5.	Identity-reveal logic not mapped to DTOs/endpoints — the audit calls for masking participants until thresholds. Add exact DTO rules and test cases.  ￼

    Immediate next-step checklist (do these in order — all small, testable)
	1.	Add event_logs table + simple logger middleware
	•	Why: captures the ML surface immediately.
	•	Test: call GET /plans and verify a VIEW_FEED row inserted.
	•	Related file: add db/migration/V##_create_event_logs.sql and src/main/java/.../EventLogService.java.  ￼
	2.	Implement real OTP flag + Twilio switch
	•	Why: safety; needed for real users.
	•	Test: toggle TWILIO_ENABLED=true and confirm SMS arrives.
	•	Related issue exists in ISSUES.md.  ￼
	3.	Add blocks & reports migrations and APIs (small PR)
	•	Why: critical safety capability.
	•	Test: create block, ensure feed hides blocked users; file a report and see DB row and admin notification.  ￼
	4.	Add contact-hash upload endpoint + store
	•	Why: enables mutuals trust signal; needed for feed UX.
	•	Test: upload list of hashes and verify contact_hashes insert count + run mutual_count query on a sample plan.  ￼
	5.	Masking / identity-reveal implementation
	•	Why: safety + UX.
	•	Test: create plan with 2 participants, fetch details as non-joined user and confirm names masked; join as 3rd user and confirm reveal per rules.  ￼
	6.	Enforce 24h “Tonight” feed filter and hub filter
	•	Why: core product hook (spontaneity and hub-density).
	•	Test: requests to GET /plans only return plans within 0–24h and in selected hub.  ￼
	7.	Wire the 8 ML events into the paths above
	•	Why: so you can start training a Phase-1 ranker after a small pilot.
	•	Test: generate a CSV export of the event_logs for 1 day of activity and verify schema matches EVENT_SCHEMAS.json.  ￼

(Those seven are your 7 “first commits” in practice. I can write exact file paths + commit messages next — you already started that in ISSUES.md.)  ￼

Concrete small commits I recommend (file + single-line change — testable)
	1.	db/migration/V01__create_event_logs.sql — Add event_logs table DDL.
	•	commit msg: chore(migrations): add event_logs table
	•	verify: psql shows table after migration.
	2.	src/main/java/com/gathr/service/EventLogService.java — Add simple service with logEvent(name, payloadJson).
	•	commit msg: feat(event): add EventLogService
	•	verify: call endpoint triggers a DB insert.
	3.	src/main/resources/application.example.yml — Add twilio.enabled: false flag and placeholder config.
	•	commit msg: chore(config): add twilio.enabled flag
	•	verify: app starts with config.
	4.	src/main/java/com/gathr/controller/AuthController.java — Wire Twilio toggle (if flag true call provider, else mock).
	•	commit msg: feat(auth): add twilio toggle
	•	verify: toggle works locally.
	5.	db/migration/V02__create_blocks_reports.sql — Add blocks/reports DDL (from audit).
	•	commit msg: chore(migrations): add blocks and reports tables
	•	verify: migrations applied.
	6.	src/main/java/com/gathr/controller/ContactsController.java — Add POST /contacts/upload endpoint (accept hashed phones).
	•	commit msg: feat(contacts): add contact-hash upload endpoint
	•	verify: upload returns processed count and rows exist.
	7.	src/main/java/com/gathr/service/PlanService.java — Add 24h & hub filter and masking logic in getPlan.
	•	commit msg: feat(plans): enforce 24h hub filter and participant masking
	•	verify: feed and detail responses show masking and only 0–24h items.

I can expand these to exact class names and code snippets if you want them staged as patch files.

⸻

ML & instrumentation: concrete next steps
	•	You already have EVENT_SCHEMAS.json; map each event to a code insertion point:
	•	VIEW_FEED → on GET /plans controller response.
	•	VIEW_ACTIVITY_DETAIL → on GET /plans/:id.
	•	JOIN_ATTEMPT → when user taps Join (pre-check).
	•	JOIN_SUCCESS → when DB insert for participation succeeds.
	•	SEND_MESSAGE → when chat message persisted.
	•	UPLOAD_CONTACTS → on contacts upload endpoint.
	•	BLOCK_USER → on block API call.
	•	APP_OPEN → on app cold start ping.
	•	(file: deliverables/EVENT_SCHEMAS.json is the source).  ￼
	•	Storage: persist to event_logs table (Postgres). Export nightly to CSV for training. Add a tiny scripts/export_events.sh to extract last-day events.
	•	Phase-1 ranker: start with heuristic candidate gen (hub + time + category match) + LightGBM using JOIN_SUCCESS as positive label.

⸻

Safety & Moderation: concrete checks to add now
	•	Implement block/report endpoints and DB (ISSUES covers this). Enforce filter in feed & chat.  ￼
	•	Implement auto-suspend rule: 2 unique reports → status = SUSPENDED for 24h. Admin UI can be simple GET /admin/reports. Include Slack/webhook alert on report creation.  ￼
	•	Keep chat logs for 30 days (per safety doc) and add a scheduled job to purge older logs.  ￼

⸻

Dev DX / CI (must do this week)
	•	Add docker-compose.yml with Postgres + app (you have a snippet in Project Audit; commit it). Run a smoke test in CI on every PR: mvn -q -DskipTests=false test + flyway migrate.  ￼
	•	Add a minimal integration test: CreatePlanAndJoinIT that runs against the docker-compose stack.

⸻

Risks to watch (and what to defer)
	•	Partner payment/ticketing automation — defer. Only surface partner perks and manual booking in alpha.  ￼
	•	Complex ML models — defer to after you have >1k events. Use heuristics + simple GBDT for now.  ￼
	•	Public profile browsing & heavy social features — defer. Keep profiles minimal and private-by-default.  ￼