# Gathr - Feature Status & Architecture

> Last Updated: November 22, 2024

## Overview

Gathr is a social platform for small-group hangouts (max 4 people). Users discover activities at local venues (hubs), join with interest/confirmation, chat anonymously until identity reveal threshold is met, then meet IRL.

**Tech Stack:**
- Backend: Spring Boot 3.2.0 (Java 17)
- Database: PostgreSQL with Flyway migrations
- Frontend: React Native + Expo (TypeScript)
- Auth: Phone OTP (Twilio/Mock) + JWT
- Real-time: WebSocket with STOMP/SockJS

---

## Feature Status

| Feature | Backend | Frontend | Status |
|---------|---------|----------|--------|
| Phone OTP Auth | ✅ | ✅ | Complete |
| User Onboarding (interests, location) | ✅ | ❌ | Backend ready |
| Hub Discovery (nearest) | ✅ | ❌ | Backend ready |
| Activity CRUD | ✅ | ✅ | Complete |
| Join/Confirm Activity | ✅ | ✅ | Complete |
| Identity Reveal (3+ confirmed) | ✅ | ✅ | Complete |
| Anonymous Chat | ✅ | ✅ | Complete |
| Invite Tokens | ✅ | ✅ | Complete |
| Post-Meet Feedback | ✅ | ❌ | Backend ready |
| Trust Score | ✅ | ❌ | Backend ready |
| Push Notifications | ✅ | ❌ | Backend ready |
| Promotions/Offers | ✅ | ❌ | Backend ready |
| Rate Limiting | ✅ | N/A | Complete |
| Block/Report Users | ✅ | ⚠️ | Partial |
| Contact Upload (mutual discovery) | ✅ | ⚠️ | Partial |

---

## Database Schema (Migrations V1-V13)

### Core Tables
```
users                 - User accounts with onboarding fields
hubs                  - Venues/locations with coordinates
activities            - Events at hubs
participations        - User-activity join records
messages              - Chat messages per activity
```

### Safety & Trust
```
blocks                - User blocking
reports               - User reports with status
feedbacks             - Post-activity ratings
```

### Monetization
```
promotions            - Partner offers with time/day targeting
promo_codes           - Discount codes
user_promotions       - Interaction tracking (view/click/save/redeem)
```

### Notifications
```
user_devices          - FCM/APNs tokens
user_notification_preferences - Per-user settings
notifications         - Delivery history
```

### Other
```
invite_tokens         - Shareable activity invites
activity_templates    - Reusable activity templates
events                - Analytics event log (JSONB)
user_phone_hashes     - Contact matching
audit_logs            - Security audit trail
```

---

## API Endpoints

### Authentication (`/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/otp/start` | Send OTP to phone |
| POST | `/auth/otp/verify` | Verify OTP, get JWT |

### User Profile (`/users`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users/me` | Get current user profile |
| PUT | `/users/me` | Update profile |
| POST | `/users/me/onboarding` | Complete onboarding |
| GET | `/users/me/onboarding-status` | Check onboarding completion |
| PUT | `/users/me/interests` | Update interests |
| PUT | `/users/me/location` | Update location |
| PUT | `/users/me/home-hub` | Set home hub |
| GET | `/users/me/trust-score` | Get trust score |
| GET | `/users/{id}/trust-score` | Get user's trust score |

### Hubs (`/hubs`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/hubs` | List all hubs |
| GET | `/hubs/{id}` | Get hub details |
| GET | `/hubs/nearest?lat=X&lng=Y` | Find nearest hubs |

### Activities (`/activities`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/activities?hubId=X` | List activities by hub |
| GET | `/activities/{id}` | Get activity details |
| POST | `/activities` | Create activity |
| POST | `/activities/{id}/join` | Join activity |
| POST | `/activities/{id}/confirm` | Confirm attendance |
| POST | `/activities/{id}/invite-token` | Generate invite link |

### Messages (`/activities/{id}/messages`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/activities/{id}/messages` | Get chat messages |
| POST | `/activities/{id}/messages` | Send message |

WebSocket: `ws://host/ws` → `/app/activities/{id}/messages`

### Feedback (`/feedbacks`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/feedbacks` | Submit feedback |
| GET | `/feedbacks/check?activityId=X` | Check if submitted |
| GET | `/feedbacks/me` | My feedback history |
| GET | `/feedbacks/activities/{id}` | Activity's feedback |
| GET | `/feedbacks/activities/{id}/stats` | Feedback statistics |
| GET | `/feedbacks/activities/{id}/mine` | My feedback for activity |

### Notifications (`/notifications`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/notifications` | Get unread notifications |
| GET | `/notifications/unread-count` | Unread count |
| POST | `/notifications/{id}/read` | Mark as read |
| GET | `/notifications/preferences` | Get settings |
| PUT | `/notifications/preferences` | Update settings |

### Device Tokens (`/devices`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/devices/register` | Register push token |
| DELETE | `/devices/{id}` | Unregister device |
| GET | `/devices` | List my devices |

### Promotions (`/promotions`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/promotions` | Active promotions |
| GET | `/promotions/{id}` | Promotion details |
| POST | `/promotions/{id}/click` | Track click |
| POST | `/promotions/{id}/save` | Save promotion |
| DELETE | `/promotions/{id}/save` | Unsave |
| GET | `/promotions/saved` | Saved promotions |
| GET | `/promotions/hub/{id}` | Hub's promotions |

### Other
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/reports` | Report a user |
| POST | `/contacts/upload` | Upload contact hashes |
| GET | `/templates` | Activity templates |
| POST | `/events` | Log analytics event |

---

## User Flows

### 1. New User Signup
```
Phone Entry → OTP Verify → JWT Token
     ↓
Check /users/me/onboarding-status
     ↓
If not completed:
  → Request location permission
  → GET /hubs/nearest (show nearby hubs)
  → Select interests (SPORTS, FOOD, ART, MUSIC)
  → Enter name
  → POST /users/me/onboarding
     ↓
Feed Screen (activities in home hub)
```

### 2. Join Activity
```
Browse Feed → Select Activity
     ↓
POST /activities/{id}/join?status=INTERESTED
     ↓
(Optional) POST /activities/{id}/confirm
     ↓
If 3+ confirmed → Identity revealed
     ↓
Chat enabled → Meet IRL
```

### 3. Post-Meet Feedback
```
Activity ends (time passes)
     ↓
App prompts feedback
     ↓
POST /feedbacks
  - didMeet: true/false
  - experienceRating: 1-5
  - wouldHangOutAgain: true/false
  - comments: "..."
     ↓
Trust score updated
```

### 4. Promotional Notification
```
Scheduler runs every 15 min
     ↓
Find active promotions matching:
  - Current day of week
  - Current time
  - User's interests
     ↓
Check user preferences:
  - Push enabled?
  - Promotional offers enabled?
  - Within quiet hours?
  - Under daily limit?
     ↓
Send via FCM/APNs
```

---

## Key Business Logic

### Identity Reveal
- Identities hidden until **3+ users CONFIRMED** (not just interested)
- Configurable via `activity.identity-reveal-threshold`
- Once revealed, cannot be un-revealed

### Trust Score Formula
```
Base 100
  + (show_ups × 5)
  - (no_shows × 10)
  + (avg_rating × 10)
  - (reports × 20)

Levels: EXCELLENT (≥150), GOOD (≥120), FAIR (≥90), LOW (≥60), POOR (<60)
```

### Rate Limiting
- Per-endpoint configurable limits
- Sliding window algorithm
- Anonymous: IP-based, Authenticated: User-based
- Returns 429 with `Retry-After` header

### Promotion Targeting
- Day of week (0=Sunday to 6=Saturday)
- Time window (minutes from midnight)
- Activity categories
- Partner tier priority (PREMIUM > BASIC > FREE)

---

## Configuration (`application.properties`)

```properties
# Database
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

# OTP Provider: mock | twilio
otp.provider=${OTP_PROVIDER:mock}
otp.expiry-minutes=5

# Twilio (if otp.provider=twilio)
twilio.account-sid=${TWILIO_ACCOUNT_SID}
twilio.auth-token=${TWILIO_AUTH_TOKEN}
twilio.phone-number=${TWILIO_PHONE_NUMBER}

# Push Provider: mock | firebase
push.provider=${PUSH_PROVIDER:mock}
firebase.server-key=${FIREBASE_SERVER_KEY}

# Activity Settings
activity.identity-reveal-threshold=3
activity.default-max-members=4

# Invite Tokens
invite.token-expiry-hours=48
```

---

## Scheduled Jobs

| Job | Frequency | Purpose |
|-----|-----------|---------|
| `MessageExpiryJob` | 15 min | Clean up expired messages |
| `PromotionalNotificationScheduler` | 15 min | Send targeted promotions |
| `ActivityReminderScheduler` | 5 min | Send 30-min reminders |

---

## Project Structure

```
src/main/java/com/gathr/
├── config/           # Security, WebSocket, WebMvc configs
├── controller/       # REST endpoints
├── dto/              # Request/Response objects
├── entity/           # JPA entities
├── exception/        # Custom exceptions
├── ratelimit/        # Rate limiting infrastructure
├── repository/       # JPA repositories
├── security/         # JWT, Auth services
└── service/          # Business logic
    └── impl/         # Service implementations

src/main/resources/
├── application.properties
└── db/migration/     # Flyway SQL migrations (V1-V13)

frontend/
├── src/
│   ├── components/   # Reusable UI components
│   ├── screens/      # Screen components
│   ├── services/     # API client
│   └── navigation/   # React Navigation setup
└── App.tsx
```

---

## What's Next (Priority Order)

1. **Frontend Onboarding Screens** - Interests picker, location permission, hub selection
2. **Frontend Feedback Screen** - Post-meet rating UI
3. **Location Verification** - Check-in mechanism for show-up verification
4. **Recommendation Engine** - Personalized activity suggestions
5. **Admin Dashboard** - Manage reports, partners, promotions
6. **Tests** - Unit/integration test coverage

---

## Environment Setup

### Backend
```bash
# Prerequisites: Java 17, PostgreSQL, Maven

# Set environment variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=gathr
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=your-secret-key

# Run
mvn spring-boot:run
```

### Frontend
```bash
# Prerequisites: Node.js, Expo CLI

cd frontend
npm install
npx expo start
```

---

## API Authentication

All endpoints except `/auth/*` require JWT token:
```
Authorization: Bearer <token>
```

Token contains `userId` as principal, extracted via `AuthenticatedUserService`.
