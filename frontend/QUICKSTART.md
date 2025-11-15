# Gatherly Frontend - Quick Start Guide

## Getting Started

1. **Install dependencies**:
```bash
cd frontend
npm install
```

2. **Start the app**:
```bash
npm start
```

3. **Run on device**:
   - Press `i` for iOS simulator
   - Press `a` for Android emulator
   - Scan QR code with Expo Go app

## Mock Mode (Default)

The app runs in **mock mode by default**, meaning it uses local mock data. No backend required!

### Testing the App

1. **Phone Entry**: Enter any 10-digit phone number (e.g., `1234567890`)
2. **OTP Verify**: Enter any OTP (e.g., `123456`) - in mock mode, any non-empty OTP works
3. **Feed Screen**: Browse activities by hub
4. **Activity Detail**: View activity details and join
5. **Chat**: Send messages (polling every 3 seconds)
6. **Create Activity**: Create new activities

## Connecting to Backend

1. Ensure backend is running on `http://localhost:8080`
2. Create `.env` file:
```
EXPO_PUBLIC_API_URL=http://localhost:8080
EXPO_PUBLIC_MOCK_MODE=false
```
3. Restart Expo: `npm start`

## Features

✅ Phone OTP authentication (mock)
✅ Hub-based activity feed
✅ Activity details with participants
✅ Mutual friend counts
✅ Group chat with polling
✅ Invite sharing
✅ Create activities
✅ Invite-only activities
✅ Safety tips and anonymity

## Project Structure

- `src/screens/` - All screen components
- `src/components/` - Reusable UI components
- `src/services/` - API service layer
- `src/types/` - TypeScript types
- `App.tsx` - Main app with navigation

## Troubleshooting

**Metro bundler cache issues**:
```bash
npx expo start --clear
```

**Android connection issues**:
```bash
adb devices
```

**iOS simulator not opening**:
- Ensure Xcode is installed (Mac only)
- Run: `open -a Simulator`

## Next Steps

1. Add WebSocket support for real-time chat
2. Implement proper date/time picker
3. Add image uploads for activities
4. Implement push notifications
5. Add user profiles

