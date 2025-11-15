# gathr Mobile App

React Native + Expo mobile app for gathr - small group hangouts around activities in Gurgaon.

## Features

- **Auth Flow**: Phone number entry with OTP verification (mock implementation)
- **Feed Screen**: "Tonight in Gurgaon" - Browse activities by hub with hub selector
- **Activity Detail**: View activity details, participants, mutual counts, and join activities
- **Group Chat**: Real-time messaging for activity groups (polling-based, WebSocket ready)
- **Invite Screen**: Share invite links and send phone invitations
- **Create Activity**: Form to create new activities with category, hub, time, and invite-only options

## Tech Stack

- **React Native** with **Expo** (~50.0.0)
- **TypeScript**
- **React Navigation** (Stack Navigator)
- **React Native Paper** (UI Components)
- **Expo SecureStore** (JWT token storage)
- **date-fns** (Date formatting)

## Prerequisites

- Node.js 18+ and npm/yarn
- Expo CLI: `npm install -g expo-cli`
- iOS Simulator (Mac) or Android Emulator, or Expo Go app on physical device

## Setup

1. **Install dependencies**:
```bash
cd frontend
npm install
```

2. **Configure API URL** (optional):
Create a `.env` file in the `frontend` directory:
```
EXPO_PUBLIC_API_URL=http://localhost:8080
EXPO_PUBLIC_MOCK_MODE=true
```

3. **Start the development server**:
```bash
npm start
```

4. **Run on device/simulator**:
- Press `i` for iOS simulator
- Press `a` for Android emulator
- Scan QR code with Expo Go app on physical device

## Mock Mode

The app runs in **mock mode by default**, which means it uses local mock data instead of making API calls to the backend. This allows you to test the app without a running backend server.

### Switching to Real API

To connect to the real backend:

1. Set `EXPO_PUBLIC_MOCK_MODE=false` in your `.env` file
2. Ensure the backend is running on `http://localhost:8080` (or update `EXPO_PUBLIC_API_URL`)
3. Restart the Expo development server

### Mock Data

The mock data includes:
- 3 hubs: Cyberhub, Galleria, 32nd Avenue
- 5 sample activities across different categories
- Sample messages for activities

## Project Structure

```
frontend/
├── src/
│   ├── components/       # Reusable UI components
│   │   ├── ActivityCard.tsx
│   │   ├── HubSelector.tsx
│   │   ├── MutualBadge.tsx
│   │   ├── AvatarAnon.tsx
│   │   └── ChatBubble.tsx
│   ├── screens/          # Screen components
│   │   ├── PhoneEntryScreen.tsx
│   │   ├── OtpVerifyScreen.tsx
│   │   ├── FeedScreen.tsx
│   │   ├── ActivityDetailScreen.tsx
│   │   ├── ChatScreen.tsx
│   │   ├── InviteScreen.tsx
│   │   └── CreateActivityScreen.tsx
│   ├── services/         # API service layer
│   │   └── api.ts
│   ├── types/            # TypeScript type definitions
│   │   └── index.ts
│   └── theme.ts          # App theme configuration
├── App.tsx               # Main app component with navigation
├── index.js              # App entry point
├── package.json
└── README.md
```

## API Endpoints

The app consumes the following backend endpoints:

### Authentication
- `POST /auth/otp/start` - Start OTP verification
- `POST /auth/otp/verify` - Verify OTP and get JWT token

### Hubs
- `GET /hubs` - Get all hubs

### Activities
- `GET /activities?hub_id={id}` - Get activities for a hub
- `GET /activities/:id` - Get activity details
- `POST /activities` - Create new activity
- `POST /activities/:id/join?status={INTERESTED|CONFIRMED}` - Join activity

### Messages
- `GET /activities/:id/messages` - Get messages for an activity
- `POST /activities/:id/messages` - Send a message

### Invites
- `POST /activities/:id/invite` - Send invitation by phone (mock)

## Key Features

### Safety & Privacy
- **Anonymity**: Participants are shown as "Member #X" until the group is confirmed (3+ participants, 1+ confirmed)
- **Mutual Counts**: Display mutual friend counts without revealing identities
- **Invite-Only Activities**: Locked activities require invitation to join
- **Safety Tips**: UI hints when participant count is low

### User Experience
- **Hub Selector**: Easy switching between different hubs (Cyberhub, Galleria, 32nd Avenue)
- **Activity Cards**: Rich cards showing category, time, participant count, and mutuals
- **Real-time Chat**: Polling-based messaging (3s interval) with WebSocket support ready
- **Ephemeral Messages**: Messages auto-clear 24 hours after activity ends (UI note)

### Navigation Flow
1. **Auth** → Phone entry → OTP verification
2. **Feed** → Browse activities → Select activity
3. **Activity Detail** → View details → Join → Chat/Invite
4. **Chat** → Real-time messaging → Confirm attendance
5. **Create Activity** → Form → Submit → View activity

## Development

### Running Tests
```bash
npm test
```

### Building for Production
```bash
expo build:android
expo build:ios
```

### Environment Variables
- `EXPO_PUBLIC_API_URL` - Backend API URL (default: http://localhost:8080)
- `EXPO_PUBLIC_MOCK_MODE` - Enable mock mode (default: true)
- `EXPO_PUBLIC_APP_URL` - App URL for invite links (default: https://gathr.app)

## Known Limitations

1. **Date Picker**: The create activity screen uses a simplified time picker. Full date/time picker can be added later.
2. **WebSocket**: Currently using polling (3s interval). WebSocket support is ready to be integrated.
3. **Mock OTP**: OTP verification accepts any non-empty string in mock mode.
4. **Participant Counts**: Backend doesn't return `peopleCount` and `mutualsCount` directly - these are calculated/mocked in the frontend.

## Troubleshooting

### Metro Bundler Issues
```bash
npx expo start --clear
```

### Android Emulator Issues
Ensure Android emulator is running and ADB is connected:
```bash
adb devices
```

### iOS Simulator Issues
Ensure Xcode and iOS Simulator are installed (Mac only).

## License

MIT

