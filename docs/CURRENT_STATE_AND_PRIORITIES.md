# Gathr: Current State & MVP Priority Roadmap

**Last Updated:** November 24, 2024
**Goal:** Ship early version ASAP
**Competitor:** [Misfits](https://www.misfits.net.in/) - See `docs/COMPETITIVE_ANALYSIS_MISFITS.md`

---

## Executive Summary

| Area | Status | Notes |
|------|--------|-------|
| **Backend** | 95% Complete | All 16 controllers functional |
| **Frontend** | 85% Complete | 13+ screens, smart feed, onboarding |
| **Database** | 100% Complete | 13 migrations, full schema |
| **Auth** | ✅ Working | OTP (Mock + Twilio), JWT |
| **Core Flow** | ✅ Working | Activities, Hubs, Join, Chat |
| **Safety** | ✅ Built | Block, Report, Trust Score |
| **Onboarding** | ✅ NEW | 4-step flow with location |
| **Smart Feed** | ✅ NEW | Interest-based filtering |
| **Location** | ✅ NEW | Auto-detect, nearest hubs |
| **Notifications** | ⚠️ Backend only | Frontend API ready |
| **Map View** | ❌ Not started | Key differentiator vs Misfits |

---

## Recent Session Progress (Nov 24, 2024)

### Completed This Session

| Feature | Files Changed | Status |
|---------|---------------|--------|
| **Location Auto-Detect** | `location.ts`, `api.ts`, `OnboardingScreen.tsx` | ✅ Done |
| **Smart Feed Filtering** | `FeedScreen.tsx`, `api.ts` | ✅ Done |
| **Enhanced Interests (8 categories)** | `OnboardingScreen.tsx`, `types/index.ts`, `theme.ts` | ✅ Done |
| **4-Step Onboarding** | `OnboardingScreen.tsx` | ✅ Done |
| **Competitive Analysis Doc** | `docs/COMPETITIVE_ANALYSIS_MISFITS.md` | ✅ Done |

### New Files Created

```
frontend/src/services/location.ts       # Location services (expo-location)
docs/COMPETITIVE_ANALYSIS_MISFITS.md    # Competitor analysis
```

### Files Modified

```
frontend/src/screens/OnboardingScreen.tsx   # 4-step flow + location
frontend/src/screens/FeedScreen.tsx         # Smart filter chips
frontend/src/services/api.ts                # feedApi endpoints
frontend/src/types/index.ts                 # Extended ActivityCategory
frontend/src/theme.ts                       # New category colors/icons
frontend/package.json                       # Added expo-location
```

---

## Current Feature Matrix

### Backend API (Spring Boot) - COMPLETE

| Feature | Endpoints | Status |
|---------|-----------|--------|
| Auth (OTP) | `/auth/otp/start`, `/auth/otp/verify` | ✅ Complete |
| Activities | CRUD, join, confirm, invite-token | ✅ Complete |
| Hubs | List, details, nearest (geo) | ✅ Complete |
| Users | Profile, onboarding, interests, location | ✅ Complete |
| Messages | REST + WebSocket + typing indicators | ✅ Complete |
| Notifications | CRUD, preferences, quiet hours | ✅ Complete |
| Push Notifications | Device registration, Firebase/Mock | ✅ Complete |
| Feedback | Submit, check, stats | ✅ Complete |
| Reports | Create with Slack webhook | ✅ Complete |
| Templates | System + user templates | ✅ Complete |
| Contacts | Privacy-preserving hash upload | ✅ Complete |
| Trust Score | Calculated from feedback/reports | ✅ Complete |
| Analytics | Event logging | ✅ Complete |
| Blocking | Block users | ✅ Complete |

### Frontend (React Native + Expo) - 85% COMPLETE

| Screen | File | Status |
|--------|------|--------|
| PhoneEntryScreen | `screens/PhoneEntryScreen.tsx` | ✅ Complete |
| OtpVerifyScreen | `screens/OtpVerifyScreen.tsx` | ✅ Complete |
| **OnboardingScreen** | `screens/OnboardingScreen.tsx` | ✅ **NEW - 4 steps** |
| FeedScreen (Home) | `screens/FeedScreen.tsx` | ✅ **Enhanced - Smart filters** |
| ActivityDetailScreen | `screens/ActivityDetailScreen.tsx` | ✅ Complete |
| ChatScreen | `screens/ChatScreen.tsx` | ✅ Complete |
| CreateActivityScreen | `screens/CreateActivityScreen.tsx` | ✅ Fixed date picker |
| TemplateSelectionScreen | `screens/TemplateSelectionScreen.tsx` | ✅ Complete |
| InviteScreen | `screens/InviteScreen.tsx` | ✅ Complete |
| ContactsUploadScreen | `screens/ContactsUploadScreen.tsx` | ✅ Complete |
| SettingsScreen | `screens/SettingsScreen.tsx` | ⚠️ Partial |
| **ProfileScreen** | `screens/ProfileScreen.tsx` | ✅ **NEW** |
| **MyActivitiesScreen** | `screens/MyActivitiesScreen.tsx` | ✅ **NEW** |
| **MapScreen** | Not created | ❌ Priority P0 |

### Frontend Components

| Component | File | Status |
|-----------|------|--------|
| ActivityCard | `components/ActivityCard.tsx` | ✅ Enhanced (capacity, joined) |
| HubSelector | `components/HubSelector.tsx` | ✅ Complete |
| InviteModal | `components/InviteModal.tsx` | ✅ Complete |
| **SkeletonLoader** | `components/SkeletonLoader.tsx` | ✅ **NEW** |

### Frontend Services

| Service | File | Status |
|---------|------|--------|
| API Client | `services/api.ts` | ✅ Enhanced (feedApi, usersApi) |
| **Location** | `services/location.ts` | ✅ **NEW** |
| Telemetry | `utils/telemetry.ts` | ✅ Complete |

---

## Activity Categories (Expanded)

Previously 4, now **8 categories**:

| Category | Key | Icon | Color |
|----------|-----|------|-------|
| Sports & Fitness | `SPORTS` | basketball | #4CAF50 |
| Food & Drinks | `FOOD` | food | #FF9800 |
| Art & Culture | `ART` | palette | #9C27B0 |
| Music & Nightlife | `MUSIC` | music | #2196F3 |
| **Outdoors & Nature** | `OUTDOOR` | tree | #8BC34A |
| **Games & Social** | `GAMES` | gamepad-variant | #E91E63 |
| **Learning & Skills** | `LEARNING` | book-open-variant | #607D8B |
| **Wellness & Mindfulness** | `WELLNESS` | meditation | #00BCD4 |

Defined in: `frontend/src/theme.ts` and `frontend/src/types/index.ts`

---

## Onboarding Flow (NEW)

4-step flow in `OnboardingScreen.tsx`:

| Step | Screen | Data Captured |
|------|--------|---------------|
| 0 | Welcome | User's name |
| 1 | Interests | Selected categories (8 options) |
| 2 | Hub Selection | Home hub + auto-detected location |
| 3 | Preferences | Group size, timing preferences |

**Location Features:**
- Auto-detect on mount using `expo-location`
- Shows nearest hubs with distance badges
- Manual "Detect My Location" button if auto fails
- Uses `hubsApi.getNearest(lat, lng)` endpoint

---

## Smart Feed (NEW)

`FeedScreen.tsx` now has filter chips:

| Filter | API Call | Description |
|--------|----------|-------------|
| For You | `feedApi.getForYou()` | Matches user interests |
| All | `activitiesApi.getByHub()` | All activities in hub |
| Mutuals | `feedApi.getWithMutuals()` | Friends attending |
| SPORTS | `feedApi.getByCategory()` | Category filter |
| FOOD | `feedApi.getByCategory()` | Category filter |
| ... | ... | All 8 categories |

**Sorting Algorithm** (`sortByInterestRelevance`):
1. Interest match (user's categories first)
2. Mutual count (friends attending)
3. Start time (soonest first)

User's interests highlighted with purple tint in filter chips.

---

## API Endpoints (Frontend)

### New in `api.ts`

```typescript
// Feed API (Smart recommendations)
feedApi.getPersonalized(hubId, date)
feedApi.getByCategory(hubId, category, date)
feedApi.getForYou(date)
feedApi.getWithMutuals(hubId, date)

// Hubs API (Location)
hubsApi.getNearest(latitude, longitude, limit)
hubsApi.getById(hubId)

// Users API
usersApi.getMe()
usersApi.updateMe(data)
usersApi.getTrustScore()
usersApi.getMyActivities()
usersApi.updateInterests(interests)

// Devices API
devicesApi.register(token, type, name)
devicesApi.unregister(deviceId)
devicesApi.getMyDevices()

// Notifications API
notificationsApi.getUnread()
notificationsApi.getUnreadCount()
notificationsApi.markAsRead(id)
notificationsApi.getPreferences()
notificationsApi.updatePreferences(prefs)
```

---

## Priority Task List

### P0: Critical (Next Sprint)

| Task | Effort | Notes |
|------|--------|-------|
| **Map View Screen** | 4-6 hrs | Key differentiator vs Misfits |
| Production deployment | 4-6 hrs | Backend + DB |
| Real Twilio OTP | 2 hrs | Currently mock |
| Push notification setup | 4-6 hrs | Firebase integration |
| End-to-end testing | 4 hrs | Real devices |

### P1: High Priority

| Task | Effort | Notes |
|------|--------|-------|
| NotificationsScreen | 3-4 hrs | API ready |
| Real-time activity pulse | 3-4 hrs | "5 joined in last hour" |
| Recommendation explainers | 2-3 hrs | "Why this?" on cards |
| Vibe tags | 2-3 hrs | Chill/Active/Social filters |

### P2: Nice to Have

| Task | Effort | Notes |
|------|--------|-------|
| Friend activity feed | 3-4 hrs | "Sarah joined Basketball" |
| Hub heat map | 2-3 hrs | Visual activity density |
| Deep linking | 3-4 hrs | Share activity links |

---

## File Structure

```
frontend/
├── App.tsx                          # Main app, navigation
├── package.json                     # Dependencies (expo ~50.0.0)
├── src/
│   ├── components/
│   │   ├── ActivityCard.tsx         # Activity list item
│   │   ├── HubSelector.tsx          # Hub picker
│   │   ├── InviteModal.tsx          # Invite code entry
│   │   └── SkeletonLoader.tsx       # Loading skeletons (NEW)
│   ├── context/
│   │   └── UserContext.tsx          # User state management
│   ├── screens/
│   │   ├── PhoneEntryScreen.tsx     # Phone input
│   │   ├── OtpVerifyScreen.tsx      # OTP verification
│   │   ├── OnboardingScreen.tsx     # 4-step onboarding (NEW)
│   │   ├── FeedScreen.tsx           # Home feed + filters (ENHANCED)
│   │   ├── ActivityDetailScreen.tsx # Activity details
│   │   ├── ChatScreen.tsx           # Activity chat
│   │   ├── CreateActivityScreen.tsx # Create activity (FIXED)
│   │   ├── TemplateSelectionScreen.tsx
│   │   ├── InviteScreen.tsx
│   │   ├── ContactsUploadScreen.tsx
│   │   ├── SettingsScreen.tsx
│   │   ├── ProfileScreen.tsx        # User profile (NEW)
│   │   └── MyActivitiesScreen.tsx   # Joined activities (NEW)
│   ├── services/
│   │   ├── api.ts                   # All API calls (ENHANCED)
│   │   └── location.ts              # Location services (NEW)
│   ├── types/
│   │   └── index.ts                 # TypeScript types (ENHANCED)
│   ├── utils/
│   │   └── telemetry.ts             # Analytics
│   └── theme.ts                     # Colors, icons (ENHANCED)
```

---

## Dependencies

### Current (`package.json`)

```json
{
  "expo": "~50.0.0",
  "expo-location": "~16.5.0",        // NEW
  "expo-notifications": "~0.27.0",
  "expo-device": "~5.9.0",
  "expo-constants": "~15.4.0",
  "expo-secure-store": "~12.8.1",
  "expo-contacts": "~12.8.1",
  "react-native-paper": "^5.11.1",
  "react-native-toast-message": "^2.1.6",
  "@react-navigation/native": "^6.1.9",
  "@react-navigation/stack": "^6.3.20",
  "date-fns": "^2.30.0",
  "axios": "^1.6.0",
  "js-sha256": "^0.11.0"
}
```

### Needed for Map View

```json
{
  "react-native-maps": "^1.8.0",
  "react-native-maps-clustering": "^3.4.2"
}
```

---

## Competitive Position vs Misfits

See `docs/COMPETITIVE_ANALYSIS_MISFITS.md` for full analysis.

| Dimension | Misfits | Gathr |
|-----------|---------|-------|
| Model | Club membership | Activity-based |
| Commitment | Weekly recurring | One-time, spontaneous |
| Discovery | List view | **Map-first** (TODO) |
| Time Focus | "Join our community" | "What's tonight?" |
| Matching | Manual browse | **Smart recommendations** ✅ |
| App Quality | Buggy (per reviews) | Solid foundation |
| Privacy | Names visible | Anonymous until confirmed |

**Key Differentiator:** Misfits is club-based (commitment). Gathr is spontaneous (tonight's plans).

---

## Quick Start for Next Session

1. **Run backend:**
   ```bash
   cd /Users/udaymukhija/gathr
   ./mvnw spring-boot:run
   ```

2. **Run frontend:**
   ```bash
   cd /Users/udaymukhija/gathr/frontend
   npm start
   ```

3. **Key files to review:**
   - `FeedScreen.tsx` - Smart feed with filters
   - `OnboardingScreen.tsx` - 4-step flow
   - `api.ts` - All API endpoints
   - `location.ts` - Location services

4. **Next priority:** MapScreen implementation

---

## Success Criteria for MVP

- [x] User can sign up with phone OTP
- [x] User can see activities in their hub
- [x] User can create an activity with date/time
- [x] User can join an activity
- [x] User can chat with participants
- [x] User can view/edit their profile
- [x] User can see their joined activities
- [x] Onboarding captures name, interests, location
- [x] Smart feed filters by interest
- [ ] User receives push notifications
- [ ] Map view for discovery
- [ ] Production deployment
- [ ] Real Twilio OTP

---

## Related Documentation

- `docs/COMPETITIVE_ANALYSIS_MISFITS.md` - Competitor analysis & strategy
- `frontend/README.md` - Frontend setup instructions
- `README.md` - Project overview
