# Gathr - Progress Report & Implementation Roadmap

**Last Updated**: November 16, 2024
**Project Status**: 60% Complete - Core MVP Built, Safety Features Needed
**Estimated Time to Launch-Ready**: 3-4 weeks

---

## Executive Summary

Gathr is an activity-first micro-hangouts app for forming small (2-4 person) groups around local activities in Gurgaon. The core MVP is functional with a solid technical foundation, but critical safety and privacy features required for public launch are missing.

**Current State**: Backend (Spring Boot) and Frontend (React Native) core flows working
**Next Phase**: Implement safety, moderation, and privacy features
**Target**: Private alpha-ready in 2 weeks, Beta-ready in 4 weeks

---

## 1. What We've Built ✅

### 1.1 Technical Infrastructure (100%)

| Component | Technology | Status | Notes |
|-----------|------------|--------|-------|
| Backend Framework | Spring Boot 3.2.0 | ✅ Complete | Java 17, production-ready |
| Database | PostgreSQL 15 | ✅ Complete | Running in Docker |
| Frontend | React Native + Expo | ✅ Complete | Cross-platform mobile |
| Authentication | JWT + Mock OTP | ✅ Complete | Ready for real SMS integration |
| Real-time Chat | WebSocket + Polling | ✅ Complete | Polling fallback implemented |
| Containerization | Docker Compose | ✅ Complete | Easy deployment |
| Testing | JUnit + Mockito | ✅ Complete | Unit tests for core services |

**Assessment**: Excellent foundation. Architecture is solid and scalable.

---

### 1.2 Core Product Features (90%)

#### ✅ Implemented Features

**Authentication Flow**
- [x] Phone number entry screen
- [x] OTP verification (mock - accepts any code)
- [x] JWT token generation and storage
- [x] Secure token storage in Expo SecureStore
- [x] Auto-login on app restart

**Hub & Activity Browsing**
- [x] Hub list endpoint (`GET /hubs`)
- [x] Hub selector UI component
- [x] "Tonight in Gurgaon" feed screen
- [x] Activity cards with category, time, participant count
- [x] Hub-based activity filtering
- [x] Activity categories: SPORTS, FOOD, ART, MUSIC

**Activity Participation**
- [x] "I'm interested" join flow
- [x] "Confirm" attendance flow
- [x] Participation status tracking (INTERESTED/CONFIRMED)
- [x] Real-time participant count updates
- [x] Activity detail screen with full info

**Group Chat**
- [x] Activity-based group chat rooms
- [x] Message sending/receiving
- [x] Polling-based updates (3s interval)
- [x] Chat bubble UI with timestamps
- [x] WebSocket infrastructure ready

**Activity Creation**
- [x] Create activity form
- [x] Category selection
- [x] Hub selection
- [x] Time window picker
- [x] Activity validation

**Data Seeding**
- [x] 3 seed hubs: Cyberhub, Galleria, 32nd Avenue
- [x] 5 sample activities across categories
- [x] Auto-seeding on application startup

---

### 1.3 Database Schema (70%)

#### ✅ Implemented Tables

**users**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) UNIQUE NOT NULL,
    verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);
```
**Status**: ✅ Core fields complete
**Missing**: `is_banned` field

---

**hubs**
```sql
CREATE TABLE hubs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    area VARCHAR(255) NOT NULL,
    description TEXT
);
```
**Status**: ✅ Complete

---

**activities**
```sql
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    hub_id BIGINT REFERENCES hubs(id),
    category VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES users(id)
);
```
**Status**: ✅ Core fields complete
**Missing**: `is_invite_only`, `created_at` fields

---

**participations**
```sql
CREATE TABLE participations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    activity_id BIGINT REFERENCES activities(id),
    status VARCHAR(50) NOT NULL, -- INTERESTED, CONFIRMED
    UNIQUE(user_id, activity_id)
);
```
**Status**: ✅ Core fields complete
**Missing**: `joined_ts`, `left_ts` timestamp tracking

---

**messages**
```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT REFERENCES activities(id),
    user_id BIGINT REFERENCES users(id),
    text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```
**Status**: ✅ Core fields complete
**Missing**: `is_deleted` field for ephemeral messages

---

### 1.4 API Endpoints (75%)

#### ✅ Working Endpoints

**Authentication**
- `POST /auth/otp/start` - Initiate OTP verification
- `POST /auth/otp/verify` - Verify OTP and receive JWT token

**Hubs**
- `GET /hubs` - List all hubs

**Activities**
- `GET /activities?hub_id={id}&date={date}` - Get activities for hub
- `POST /activities` - Create new activity
- `POST /activities/{id}/join?status={INTERESTED|CONFIRMED}` - Join activity

**Messages**
- `GET /activities/{id}/messages` - Get chat messages
- `POST /activities/{id}/messages` - Send message

**Invites**
- `POST /activities/{id}/invite` - Send invitation (mocked)

---

## 2. What's Missing ❌

### 2.1 Critical Safety Features (0% Complete)

#### 🔴 **Blocking System** - CRITICAL FOR LAUNCH

**Why It Matters**: Users need to block harassers and creeps immediately.

**Missing Components**:
- [ ] `blocks` database table
- [ ] `Block` entity class
- [ ] `BlockRepository` interface
- [ ] `POST /blocks` endpoint
- [ ] `DELETE /blocks/{userId}` endpoint (unblock)
- [ ] Block checking in activity feeds
- [ ] Block checking in chat messages
- [ ] Block button in UI

**User Story**: *"As a user, I want to block someone who made me uncomfortable, so they can't see my activities or message me."*

---

#### 🔴 **Reporting System** - CRITICAL FOR LAUNCH

**Why It Matters**: Platform cannot operate without user reporting and moderation.

**Missing Components**:
- [ ] `reports` database table
- [ ] `Report` entity class with status (PENDING, REVIEWED, RESOLVED)
- [ ] `ReportRepository` interface
- [ ] `POST /reports` endpoint
- [ ] `GET /reports` endpoint (admin only)
- [ ] `PUT /reports/{id}/status` endpoint (admin review)
- [ ] Report button in chat UI
- [ ] Report reason selection (harassment, spam, fake profile, etc.)
- [ ] Auto-ban logic after 2+ reports

**User Story**: *"As a user, I want to report inappropriate messages, so moderators can take action."*

---

### 2.2 High-Priority Privacy Features (20% Complete)

#### 🟡 **Mutuals Count System**

**Why It Matters**: Trust signal - shows shared connections without revealing identities.

**Current State**: Frontend shows mocked mutuals count
**Missing Components**:
- [ ] Contact hash storage strategy
- [ ] Mutuals count calculation service
- [ ] Contact sync endpoint
- [ ] Privacy-preserving hash matching algorithm
- [ ] Update activity detail to include real mutuals_count
- [ ] User consent flow for contact access

**User Story**: *"As a user, I want to see how many mutual friends I have with participants without revealing who they are."*

---

#### 🟡 **Identity Reveal Logic**

**Why It Matters**: Core privacy promise - stay anonymous until group confirms.

**Current State**: Names are always visible
**Missing Components**:
- [ ] Anonymization service ("Member #1", "Member #2")
- [ ] Threshold configuration (default: 3+ participants, 1+ confirmed)
- [ ] Conditional name reveal in API responses
- [ ] Frontend anonymized avatar/name display
- [ ] Gender-based reveal rules (women can stay anonymous)

**User Story**: *"As a woman, I want my name hidden until I'm sure the group is real, so I feel safer."*

---

### 2.3 Medium-Priority Access Control (10% Complete)

#### 🟡 **Invite-Only Activities**

**Why It Matters**: Enables controlled, safe rollout and private events.

**Missing Components**:
- [ ] `invite_tokens` database table
- [ ] `InviteToken` entity class with expiration
- [ ] `InviteTokenRepository` interface
- [ ] Invite link generation service
- [ ] Real SMS invite sending (Twilio/MSG91)
- [ ] Invite token validation middleware
- [ ] Activity access control (reject non-invited users)
- [ ] Lock icon UI for invite-only activities
- [ ] "Request Invite" flow for locked activities

**User Story**: *"As an organizer, I want to create invite-only activities for my close friends."*

---

#### 🟡 **+1 Guest Feature**

**Why It Matters**: Safety mechanism - bring a friend to first meetup.

**Missing Components**:
- [ ] `+1` toggle in create activity UI
- [ ] `brings_plus_one` field in participations table
- [ ] Group size enforcement (max 4 users + 4 guests = 8 total)
- [ ] +1 count display in UI
- [ ] +1 explanation in activity detail

**User Story**: *"As a first-time user, I want to bring my friend to feel safer at the meetup."*

---

### 2.4 Lower-Priority Polish Features

#### 🟢 **Ephemeral Messages** (LOW PRIORITY)

**Why It Matters**: Privacy compliance - messages auto-delete.

**Missing Components**:
- [ ] Scheduled job to mark old messages as deleted
- [ ] Deletion policy: 24 hours after activity end
- [ ] Soft delete implementation (mark `is_deleted = true`)
- [ ] Filter deleted messages from API responses

---

#### 🟢 **Push Notifications** (LOW PRIORITY)

**Why It Matters**: Engagement - notify when room hits threshold.

**Missing Components**:
- [ ] FCM (Firebase Cloud Messaging) integration
- [ ] Device token storage
- [ ] Push notification service
- [ ] Notification triggers:
  - Room hits 3+ interested
  - Someone confirms your activity
  - You're invited to activity
  - New message in your group

---

#### 🟢 **Admin Dashboard** (LOW PRIORITY)

**Why It Matters**: Moderation tooling for scaling.

**Missing Components**:
- [ ] Admin role in users table
- [ ] Admin authentication
- [ ] Moderation queue UI
- [ ] Ban/unban user controls
- [ ] Activity statistics dashboard
- [ ] Report review interface

---

#### 🟢 **Post-Meet Feedback** (LOW PRIORITY)

**Why It Matters**: Collect success metrics and trust scores.

**Missing Components**:
- [ ] Feedback popup 2 hours after activity end
- [ ] "Did you meet?" (Yes/No)
- [ ] "Would you meet them again?" (Yes/No)
- [ ] "Any safety issues?" (report link)
- [ ] Feedback storage and analytics

---

## 3. Detailed Roadmap

### Phase 1: Critical Safety (Week 1-2) 🔴

**Goal**: Make app safe for real users

**Tasks**:
1. **Database Changes** (Day 1)
   - [ ] Add `users.is_banned` field
   - [ ] Create `blocks` table
   - [ ] Create `reports` table
   - [ ] Update existing entities

2. **Backend Implementation** (Day 2-5)
   - [ ] Create `Block` entity and repository
   - [ ] Create `Report` entity and repository
   - [ ] Implement `BlockService` with block checking
   - [ ] Implement `ReportService` with auto-ban logic
   - [ ] Create `POST /blocks` endpoint
   - [ ] Create `POST /reports` endpoint
   - [ ] Create `GET /reports` endpoint (admin)
   - [ ] Add block filtering to activity feed
   - [ ] Add block filtering to chat

3. **Frontend Implementation** (Day 6-8)
   - [ ] Add block button in user profiles
   - [ ] Add report button in chat
   - [ ] Add report reason selection modal
   - [ ] Add "User blocked" confirmation UI
   - [ ] Add "Report submitted" confirmation UI

4. **Testing** (Day 9-10)
   - [ ] Unit tests for BlockService
   - [ ] Unit tests for ReportService
   - [ ] Integration tests for block/report flows
   - [ ] Manual testing of moderation flows

**Success Metrics**:
- Users can block others
- Users can report harassment
- Auto-ban triggers after 2 reports
- Blocked users don't appear in feeds

---

### Phase 2: Privacy & Trust (Week 3-4) 🟡

**Goal**: Implement privacy promises from spec

**Tasks**:
1. **Invite-Only Activities** (Day 11-13)
   - [ ] Create `invite_tokens` table
   - [ ] Create `InviteToken` entity and repository
   - [ ] Add `is_invite_only` field to activities
   - [ ] Implement invite link generation
   - [ ] Add access control middleware
   - [ ] Update UI with lock icons
   - [ ] Implement "Request Invite" flow

2. **Identity Reveal Logic** (Day 14-16)
   - [ ] Create anonymization service
   - [ ] Configure reveal threshold (3+ participants)
   - [ ] Update activity detail API to anonymize names
   - [ ] Update frontend to show "Member #1"
   - [ ] Add conditional reveal logic
   - [ ] Test anonymization flows

3. **Missing Field Updates** (Day 17-18)
   - [ ] Add `activity.created_at` field
   - [ ] Add `participation.joined_ts` field
   - [ ] Add `participation.left_ts` field
   - [ ] Add `message.is_deleted` field
   - [ ] Update DTOs and mappers

4. **+1 Guest Feature** (Day 19-20)
   - [ ] Add `participation.brings_plus_one` field
   - [ ] Add +1 toggle in create activity UI
   - [ ] Implement group size validation
   - [ ] Update participant count display
   - [ ] Add +1 explanation text

**Success Metrics**:
- Invite-only activities work end-to-end
- Names are hidden until threshold met
- +1 feature functional
- All timestamp fields tracked

---

### Phase 3: Polish & Scale Prep (Week 5-6) 🟢

**Goal**: Production-ready polish

**Tasks**:
1. **Ephemeral Messages** (Day 21-22)
   - [ ] Create scheduled job for message cleanup
   - [ ] Implement soft delete (is_deleted flag)
   - [ ] Add deletion policy (24h after activity end)
   - [ ] Test automated cleanup

2. **Push Notifications** (Day 23-25)
   - [ ] Integrate FCM SDK
   - [ ] Add device token storage
   - [ ] Implement notification service
   - [ ] Add notification triggers
   - [ ] Test push delivery

3. **Admin Tools** (Day 26-28)
   - [ ] Create admin UI (simple web dashboard)
   - [ ] Implement moderation queue
   - [ ] Add ban/unban controls
   - [ ] Add activity stats dashboard

4. **Post-Meet Feedback** (Day 29-30)
   - [ ] Create feedback popup trigger
   - [ ] Implement feedback storage
   - [ ] Add analytics dashboard
   - [ ] Test feedback flow

**Success Metrics**:
- Old messages auto-delete
- Push notifications working
- Admin can review reports
- Feedback collection working

---

## 4. Updated Data Model (Target State)

### Complete Entity Diagram

```
users
├── id (PK)
├── name
├── phone (unique)
├── verified
├── is_banned ← NEW
└── created_at

hubs
├── id (PK)
├── name
├── area
└── description

activities
├── id (PK)
├── title
├── hub_id (FK)
├── category
├── start_time
├── end_time
├── created_by (FK)
├── is_invite_only ← NEW
└── created_at ← NEW

participations
├── id (PK)
├── user_id (FK)
├── activity_id (FK)
├── status (INTERESTED/CONFIRMED)
├── joined_ts ← NEW
├── left_ts ← NEW
└── brings_plus_one ← NEW

messages
├── id (PK)
├── activity_id (FK)
├── user_id (FK)
├── text
├── created_at
└── is_deleted ← NEW

blocks ← NEW TABLE
├── id (PK)
├── blocker_id (FK → users)
├── blocked_id (FK → users)
└── created_at

reports ← NEW TABLE
├── id (PK)
├── reporter_id (FK → users)
├── target_user_id (FK → users)
├── message_id (FK → messages, nullable)
├── activity_id (FK → activities, nullable)
├── reason (TEXT)
├── status (PENDING/REVIEWED/RESOLVED)
└── created_at

invite_tokens ← NEW TABLE
├── id (PK)
├── activity_id (FK)
├── token (unique)
├── expires_at
└── created_at

audit_log ← NEW TABLE (future)
├── id (PK)
├── actor_id (FK → users)
├── action
├── entity
├── entity_id
└── timestamp
```

---

## 5. Risk Assessment & Mitigation

### Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Auto-ban too aggressive | Medium | High | Add admin review queue |
| Mutuals calculation slow | Low | Medium | Cache results, async processing |
| Push notifications fail | Medium | Low | Graceful degradation, polling fallback |
| WebSocket scale issues | Low | Medium | Start with polling, add WS later |

### Product Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Not enough users in hub | High | Critical | Seed with fake activities, invite-only start |
| Creeps overwhelm platform | Medium | Critical | Invite-only + aggressive moderation |
| Users leak to WhatsApp | High | Medium | Build plan-formation features in-app |
| Women feel unsafe | Medium | Critical | Prioritize safety features (Phase 1) |

---

## 6. Success Metrics (Post-Launch)

### North Star Metric
**% of activities that result in real meetups**

### Supporting Metrics

**Activation**
- Target: 40% of signups join their first activity within 7 days
- Current: TBD (need to launch)

**Room Formation**
- Target: 30% of activities hit 3+ interested within 60 minutes
- Current: TBD (need real data)

**Conversion**
- Target: 25% of joined activities convert to confirmed meetups
- Current: TBD (need feedback loop)

**Safety**
- Target: <1 report per 100 rooms
- Current: 0 (no reporting system yet)

**Retention**
- Target: 30% day-7 retention for users who joined ≥1 activity
- Current: TBD

---

## 7. Pre-Launch Checklist

### Before Private Alpha (Week 2)
- [ ] Block system working
- [ ] Report system working
- [ ] Auto-ban after 2 reports
- [ ] All safety features tested
- [ ] Invite-only mode enabled
- [ ] 20-30 seed users recruited
- [ ] 1-2 hubs activated
- [ ] Venue partnerships secured

### Before Closed Beta (Week 4)
- [ ] Identity reveal logic working
- [ ] Invite-only activities functional
- [ ] +1 feature implemented
- [ ] Push notifications working
- [ ] 3-5 hubs activated
- [ ] 100-200 users onboarded
- [ ] Moderation queue operational

### Before Public Launch (Week 8+)
- [ ] Ephemeral messages working
- [ ] Admin dashboard functional
- [ ] Post-meet feedback collecting
- [ ] Metrics dashboard live
- [ ] All privacy policies updated
- [ ] Legal review complete
- [ ] 500+ users ready
- [ ] All hubs activated

---

## 8. What We're NOT Doing (Yet)

### Deferred to V2
- ML/AI features (room viability prediction, recommendations)
- Vibe Mode (spontaneous check-ins)
- Venue revenue sharing
- Ticketing for paid events
- Full contact sync with hashing
- Advanced analytics
- Multi-city expansion
- iOS/Android native apps (using Expo for now)

### Intentionally Excluded
- Dating features
- 1:1 DMs
- Profile browsing
- Large group events (>4 people)
- Public activities (all start invite-only)

---

## 9. Team & Resources

### Current Team
- **Solo founder/developer**: Building MVP

### Recommended Hires (Post-Alpha)
1. **Full-stack engineer** - Help with backend features
2. **Mobile developer** - Polish frontend UX
3. **Part-time community manager** - Seeding, moderation, partnerships

### Budget (Estimated Monthly)
- Infrastructure (Render/AWS): $100-300
- SMS (Twilio): $50-100
- Push notifications (FCM): Free tier
- **Total**: ~$200-400/month for beta

---

## 10. Next Immediate Actions

### This Week (Days 1-7)

**Monday-Tuesday**: Database Schema Updates
- Add `is_banned` to users table
- Create `blocks` table
- Create `reports` table
- Write migration scripts

**Wednesday-Friday**: Backend Implementation
- Create Block entity, repository, service
- Create Report entity, repository, service
- Implement POST /blocks endpoint
- Implement POST /reports endpoint
- Add auto-ban logic

**Weekend**: Frontend Integration
- Add block button UI
- Add report button UI
- Test end-to-end flows

---

## Conclusion

**Current Status**: Strong technical foundation (60% complete)
**Critical Path**: Implement safety features (2 weeks)
**Timeline to Alpha**: 2 weeks
**Timeline to Beta**: 4 weeks

The project is in excellent shape. The core product works, and the architecture is solid. The missing pieces are well-defined and achievable. With focused execution on safety features first, this can be alpha-ready in 2 weeks.

**Recommendation**: Proceed immediately with Phase 1 (Critical Safety Features). Do not launch to real users until blocking and reporting systems are functional.

---

**Document Version**: 1.0
**Next Review**: After Phase 1 completion (Week 2)
