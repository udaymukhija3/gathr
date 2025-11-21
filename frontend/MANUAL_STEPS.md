# Manual Steps to Run Gatherly Frontend

## Quick Start

1. **Install dependencies:**
   ```bash
   cd frontend
   npm install
   ```

2. **Create `.env` file:**
   ```bash
   # Copy this content to .env file
   EXPO_PUBLIC_API_URL=http://localhost:8080
   EXPO_PUBLIC_WS_URL=ws://localhost:8080
   EXPO_PUBLIC_MOCK_MODE=false
   ```

3. **Start Expo:**
   ```bash
   npm start
   # or
   expo start
   ```

4. **Run on device:**
   - Scan QR code with Expo Go app
   - Or press `i` (iOS) / `a` (Android) / `w` (web)

## Environment Variables Setup

Create a `.env` file in the `frontend` directory with:

```bash
# Backend API URL
EXPO_PUBLIC_API_URL=http://localhost:8080

# WebSocket URL
EXPO_PUBLIC_WS_URL=ws://localhost:8080

# Mock mode (true = use mock data, false = use real API)
EXPO_PUBLIC_MOCK_MODE=false
```

**Important:** After changing `.env`, restart Expo server.

## Mock Mode Testing

To test without backend:

1. Set `EXPO_PUBLIC_MOCK_MODE=true` in `.env`
2. Restart Expo server
3. App will use mock data from `src/mock/data.json`

Mock mode includes:
- Mock authentication (OTP always "123456")
- 3 hubs and 5 sample activities
- Mock messages and participants

## Testing Contact Upload

1. **Grant permissions:**
   - iOS: Settings > Privacy > Contacts > Gatherly
   - Android: App Settings > Permissions > Contacts

2. **Test flow:**
   - Navigate to Settings
   - Enable Contact Upload
   - Grant permission when prompted
   - App hashes contacts client-side
   - Only hashes sent to backend

3. **Verify:**
   - Check console logs for hash array
   - Backend should return `mutualsCount`
   - Count appears in activity participant cards

## Testing Multi-Device Flows

1. **Start Expo on your machine:**
   ```bash
   expo start
   ```

2. **Device 1 (User A):**
   - Scan QR code
   - Login with phone: `1234567890`
   - OTP: `123456` (mock mode)
   - Create an activity

3. **Device 2 (User B):**
   - Scan QR code
   - Login with phone: `9876543210`
   - OTP: `123456` (mock mode)
   - Join User A's activity
   - Enter chat room

4. **Test real-time:**
   - User A sends message
   - Should appear on User B's screen (WebSocket or polling)

## Backend Integration

Before connecting to real backend:

1. **Verify backend is running:**
   ```bash
   curl http://localhost:8080/hubs
   ```

2. **Check CORS:**
   - Backend must allow `http://localhost:19006`
   - Check backend CORS configuration

3. **Set environment:**
   ```bash
   EXPO_PUBLIC_MOCK_MODE=false
   EXPO_PUBLIC_API_URL=http://localhost:8080
   ```

4. **Test endpoints:**
   - See `BACKEND_INTEGRATION.md` for endpoint checklist
   - Verify all endpoints return expected JSON

## Common Issues

### WebSocket Connection Fails
- **Symptom:** Chat doesn't update in real-time
- **Solution:** App automatically falls back to polling every 3 seconds
- **Check:** Backend WebSocket endpoint is running

### Rate Limiting (429)
- **Symptom:** "Too many requests" error on OTP
- **Solution:** Wait 1 hour or test with different phone number
- **Expected:** Max 3 OTP requests per hour per phone

### Contacts Permission Denied
- **Symptom:** Can't upload contacts
- **Solution:** 
  - iOS: Settings > Privacy > Contacts
  - Android: App Settings > Permissions
  - Grant permission and retry

### Mock Mode Not Working
- **Check:** `EXPO_PUBLIC_MOCK_MODE=true` in `.env`
- **Restart:** Expo server after changing env vars
- **Verify:** `src/mock/data.json` exists

## Production Build

1. **Build for production:**
   ```bash
   expo build:android
   # or
   expo build:ios
   ```

2. **Update environment:**
   - Set production API URL
   - Set `EXPO_PUBLIC_MOCK_MODE=false`
   - Configure production WebSocket URL

3. **Test:**
   - Install on physical device
   - Test all flows end-to-end
   - Verify real-time features work

## Next Steps

1. ✅ Install dependencies
2. ✅ Set up `.env` file
3. ✅ Test in mock mode
4. ✅ Test contact upload
5. ✅ Test multi-device flows
6. ⏭️ Connect to backend (see `BACKEND_INTEGRATION.md`)
7. ⏭️ Test all endpoints
8. ⏭️ Deploy to production

