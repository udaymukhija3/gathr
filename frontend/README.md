# Gatherly Mobile App

React Native + Expo frontend for Gatherly - small group hangouts around local activities.

## Tech Stack

- **Framework**: React Native + Expo (~50.0.0)
- **Language**: TypeScript
- **UI Library**: React Native Paper
- **Navigation**: React Navigation (Stack)
- **State Management**: React Context + Hooks
- **Networking**: Axios (REST), WebSocket (real-time with polling fallback)
- **Storage**: Expo SecureStore (JWT tokens)
- **Contact Hashing**: js-sha256

## Features

- ✅ Phone OTP authentication with rate limiting
- ✅ Hub-based activity discovery
- ✅ Activity creation and joining
- ✅ Invite-only activities with token system
- ✅ Real-time chat (WebSocket + polling fallback)
- ✅ Mutual contacts discovery (privacy-first hashing)
- ✅ Identity reveal logic (server-authoritative)
- ✅ Max group size enforcement
- ✅ Message expiry countdown
- ✅ Report & moderation
- ✅ Mock mode for offline development

## Prerequisites

- Node.js 18+ and npm/yarn
- Expo CLI: `npm install -g expo-cli`
- iOS Simulator (Mac) or Android Emulator / Physical device
- Backend API running (or use mock mode)

## Installation

1. **Clone and navigate to frontend:**
```bash
cd frontend
npm install
```

2. **Set up environment variables:**
Create a `.env` file in the `frontend` directory (copy from `.env.example`):
```bash
# API Configuration
EXPO_PUBLIC_API_URL=http://localhost:8080
EXPO_PUBLIC_WS_URL=ws://localhost:8080

# Mock Mode (optional - defaults to false for production)
# Set to 'true' to use mock data for offline development
# Omit or set to 'false' to use real API
EXPO_PUBLIC_MOCK_MODE=true  # Remove this line for production
```

3. **Start the development server:**
```bash
npm start
# or
expo start
```

4. **Run on device:**
- Scan QR code with Expo Go app (iOS/Android)
- Press `i` for iOS simulator
- Press `a` for Android emulator
- Press `w` for web browser

## Mock Mode

To run entirely offline without backend:

1. Set `EXPO_PUBLIC_MOCK_MODE=true` in `.env`
2. All API calls will use local mock data instead of hitting the backend

**IMPORTANT**:
- **Production builds default to real API mode** (mock mode OFF)
- Only enable mock mode explicitly for offline development
- Never deploy with `EXPO_PUBLIC_MOCK_MODE=true`

Mock mode includes:
- 3 hubs (Cyberhub, Galleria, 32nd Avenue)
- 5 sample activities
- Mock authentication (always accepts OTP "123456")
- Mock messages and participants

## Testing Contact Upload

1. **Grant contacts permission:**
   - iOS: Settings > Privacy > Contacts > Gatherly
   - Android: App permissions > Contacts

2. **Test with sample numbers:**
   - The app normalizes phone numbers to E.164 format
   - Hashes are computed client-side using SHA-256
   - Only hashes are sent to backend (never raw contacts)

3. **Sample test flow:**
   ```typescript
   // Example: Phone "+919876543210" becomes hash
   // Hash is sent: ["abc123...", "def456..."]
   // Backend returns: { mutualsCount: 3 }
   ```

## Testing Multi-Device Flows

1. **Start Expo on your machine:**
   ```bash
   expo start
   ```

2. **Connect two devices:**
   - Device 1: Scan QR code, login as User A
   - Device 2: Scan QR code, login as User B

3. **Test join → chat flow:**
   - User A creates activity
   - User B joins activity
   - Both users enter chat room
   - Send messages (should appear in real-time)

4. **Test invite token flow:**
   - User A creates invite-only activity
   - User A generates invite token
   - User B enters token to join

## Project Structure

```
frontend/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── ActivityCard.tsx
│   │   ├── AvatarAnon.tsx
│   │   ├── HubSelector.tsx
│   │   ├── InviteModal.tsx
│   │   └── MutualBadge.tsx
│   ├── screens/             # Screen components
│   │   ├── PhoneEntryScreen.tsx
│   │   ├── OtpVerifyScreen.tsx
│   │   ├── FeedScreen.tsx (HomeScreen)
│   │   ├── ActivityDetailScreen.tsx
│   │   ├── ChatScreen.tsx
│   │   ├── CreateActivityScreen.tsx
│   │   ├── ContactsUploadScreen.tsx
│   │   └── SettingsScreen.tsx
│   ├── context/             # React Context
│   │   └── UserContext.tsx
│   ├── hooks/               # Custom hooks
│   │   ├── useApi.ts
│   │   └── useWebSocket.ts
│   ├── services/            # API services
│   │   └── api.ts
│   ├── utils/               # Utilities
│   │   └── contactHashing.ts
│   ├── types/               # TypeScript types
│   │   └── index.ts
│   ├── mock/                # Mock data
│   │   └── data.json
│   └── theme.ts             # Theme configuration
├── App.tsx                  # Root component
├── package.json
├── tsconfig.json
└── README.md
```

## Location & Nearby Discovery

- Creators can either choose from curated hubs or switch to **Pick any place** to search real venues (Mapbox Places) or type coordinates manually.
- Setting `EXPO_PUBLIC_MAPBOX_TOKEN` enables autocomplete + address suggestions in the create flow.
- The feed now supports a **Near Me** mode that requests location permission, lets users pick a radius (5/10/25 km), and surfaces events based on coordinates returned from the API.
- Backend requests now send either `hubId` or `{ placeName, latitude, longitude, address }` so radius searches can work consistently.

## API Endpoints (Backend Integration)

The frontend expects these backend endpoints:

### Authentication
- `POST /auth/otp/start` - Start OTP (rate limited: 3/hour)
  - Body: `{ phone: string }`
  - Returns: `{ message: string }` or `429 Too Many Requests`

- `POST /auth/otp/verify` - Verify OTP
  - Body: `{ phone: string, otp: string }`
  - Returns: `{ token: string, user: User }`

### Activities
- `GET /activities?hub_id=<id>&date=<YYYY-MM-DD>` - Get activities for a hub
- `GET /activities?latitude=<lat>&longitude=<lng>&radiusKm=<km>` - Get activities near a coordinate
  - Returns: `Activity[]` with `peopleCount`, `mutualsCount`, `isInviteOnly`, `revealIdentities`, `maxMembers`, `locationName`, `locationAddress`, `distanceKm`

- `GET /activities/:id` - Get activity details
  - Returns: `ActivityDetail` with `participants[]` (anon if `revealIdentities=false`)

- `POST /activities` - Create activity
  - Body: Either `{ title, hubId, category, startTime, endTime, isInviteOnly?, maxMembers? }` **or**
    `{ title, category, startTime, endTime, placeName, latitude, longitude, placeAddress?, isInviteOnly?, maxMembers? }`

- `POST /activities/:id/join?status=INTERESTED|CONFIRMED&inviteToken=<token>` - Join activity
  - Returns: `409 Conflict` if max members reached
  - Returns: `403 Forbidden` if invite token missing/invalid

- `POST /activities/:id/invite-token` - Generate invite token
  - Returns: `{ token: string, expiresAt: string }`

- `POST /activities/:id/confirm` - Confirm participation (heading now)

### Messages
- `GET /activities/:id/messages?since=<timestamp>` - Get messages (polling)
  - Returns: `Message[]`

- `POST /activities/:id/messages` - Send message
  - Body: `{ text: string }`

### WebSocket
- `ws://<base>/ws/activities/:id?token=<jwt>` - Real-time messages
  - Messages: `{ type: 'message'|'join'|'leave'|'heading_now'|'reveal', payload: {...} }`

### Reports
- `POST /reports` - Create report
  - Body: `{ targetUserId: number, activityId?: number, reason: string }`

### Contacts
- `POST /contacts/upload` - Upload hashed contacts
  - Body: `{ hashes: string[] }`
  - Returns: `{ mutualsCount: number }`

## Key Features Implementation

### Invite Token Flow
1. User creates invite-only activity
2. User generates invite token via `POST /activities/:id/invite-token`
3. Token shared via SMS or link
4. Recipient enters token when joining
5. Backend validates token (expiry, validity)

### Identity Reveal Logic
- Frontend displays based on `revealIdentities` flag from backend
- If `false`: Shows anonymized avatars + mutualsCount
- If `true`: Shows firstName + avatar
- Server controls reveal (>=3 confirmed OR >=3 interested)

### Max Group Size
- Frontend disables join button if `peopleCount >= maxMembers`
- Shows "Full" message
- Backend enforces with `409 Conflict` response

### Message Expiry
- Calculates: `activity.endTime + 24h`
- Shows countdown in chat header
- Messages auto-deleted after expiry (backend job)

### Mutual Contacts
- User grants contacts permission
- Phone numbers normalized to E.164
- Hashed client-side with SHA-256
- Only hashes sent to backend
- Backend returns intersection count
- Displayed as "X mutuals" (never reveals who)

## Running Tests

```bash
npm test
```

Tests cover:
- ActivityCard rendering (locked/unlocked)
- Anonymity → reveal behavior
- Invite token validation UI

## Environment Variables

| Variable | Description | Default | Production |
|----------|-------------|---------|------------|
| `EXPO_PUBLIC_API_URL` | Backend API base URL | `http://localhost:8080` | Set to production API URL |
| `EXPO_PUBLIC_WS_URL` | WebSocket base URL | `ws://localhost:8080` | Set to production WS URL |
| `EXPO_PUBLIC_MOCK_MODE` | Use mock data (dev only) | `false` | **Do not set** (defaults to false) |
| `EXPO_PUBLIC_MAPBOX_TOKEN` | Mapbox Places API token for location search | _unset_ | Required to enable map/location search when creating activities |

## Troubleshooting

### WebSocket Connection Issues
- Check `EXPO_PUBLIC_WS_URL` is correct
- Verify backend WebSocket endpoint is running
- App automatically falls back to polling if WS fails

### Rate Limiting (OTP)
- Frontend shows friendly error for 429 responses
- Rate limit: 3 requests per hour per phone
- Error message: "Too many requests. Please try again later."

### Contacts Permission Denied
- iOS: Settings > Privacy > Contacts
- Android: App Settings > Permissions
- App shows error toast if permission denied

### Mock Mode Not Working
- Ensure `EXPO_PUBLIC_MOCK_MODE=true` in `.env` (must be exactly 'true')
- Restart Expo server after changing env vars (kill and restart `npm start`)
- Verify mock data exists in `src/services/api.ts` (MOCK_HUBS, MOCK_ACTIVITIES)

### Real API Not Working (Getting Mock Data in Production)
- Remove `EXPO_PUBLIC_MOCK_MODE=true` from `.env` or set to 'false'
- Check API_BASE_URL is pointing to the correct backend
- Clear Expo cache: `expo start -c`

## Backend Integration Checklist

Before connecting to real backend, ensure:

- [ ] Backend has all endpoints listed above
- [ ] CORS configured for Expo dev server (`http://localhost:19006`, etc.)
- [ ] WebSocket endpoint accepts JWT in query param
- [ ] Rate limiting returns proper 429 status
- [ ] Invite token validation returns 403 for invalid tokens
- [ ] Max members enforcement returns 409 when full
- [ ] Contact upload endpoint accepts `{ hashes: string[] }`
- [ ] Activity responses include all new fields (`revealIdentities`, `maxMembers`, etc.)

## Development Notes

- **State Management**: Uses React Context for user/auth state, local state for UI
- **Error Handling**: Global error handler shows toast notifications
- **Offline Support**: Mock mode allows full offline development
- **Real-time**: WebSocket with automatic polling fallback
- **Security**: JWT stored in SecureStore, never logged

## License

MIT
