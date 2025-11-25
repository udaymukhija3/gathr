# 🚀 LAUNCH GATHR IN 4 HOURS

## Prerequisites (Install Once)
```bash
# Install Railway CLI
brew install railway

# Install EAS CLI
npm install -g eas-cli
```

---

## STEP 1: Deploy Backend to Railway (90 minutes)

### 1a. Initialize Railway Project (5 mins)
```bash
cd /Users/udaymukhija/gathr
railway login
railway init
# Select: "Create new project" → Name it "gathr-backend"
```

### 1b. Add PostgreSQL Database (2 mins)
```bash
railway add postgresql
# Railway auto-creates DB and sets DATABASE_URL
```

### 1c. Set Environment Variables (5 mins)
```bash
# Copy-paste these:
railway variables set JWT_SECRET="gathr-production-secret-key-change-this-minimum-256-bits-long-string"
railway variables set OTP_PROVIDER="mock"
railway variables set CORS_ALLOWED_ORIGINS="*"
railway variables set APP_URL="https://gathr.app"
railway variables set SPRING_PROFILES_ACTIVE="prod"
```

### 1d. Create railway.json Config (2 mins)
Create `/Users/udaymukhija/gathr/railway.json`:
```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS",
    "buildCommand": "mvn clean package -DskipTests"
  },
  "deploy": {
    "startCommand": "java -jar target/gathr-backend-1.0.0.jar",
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 100,
    "restartPolicyType": "ON_FAILURE"
  }
}
```

### 1e. Deploy! (70 mins - Maven build is slow)
```bash
railway up
# Watch logs: railway logs --follow
```

### 1f. Get Your Backend URL (1 min)
```bash
railway domain
# Copy the URL (e.g., https://gathr-backend-production.up.railway.app)
```

### 1g. Test It Works (5 mins)
```bash
# Replace with your Railway URL:
BACKEND_URL="https://gathr-backend-production.up.railway.app"

# Test health check
curl $BACKEND_URL/actuator/health
# Should return: {"status":"UP"}

# Test OTP start (mock mode)
curl -X POST $BACKEND_URL/auth/otp/start \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919999999999"}'
# Should return 200 OK

# Test OTP verify (mock accepts any 6-digit code)
curl -X POST $BACKEND_URL/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919999999999","otp":"123456"}'
# Should return JWT token
```

✅ **Backend is LIVE! Save your BACKEND_URL for next step.**

---

## STEP 2: Configure Frontend (15 minutes)

### 2a. Update API Base URL (2 mins)
Edit `/Users/udaymukhija/gathr/frontend/src/services/api.ts`:

Find line ~25:
```typescript
const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';
```

Change to your Railway URL:
```typescript
const API_BASE_URL = 'https://gathr-backend-production.up.railway.app';
```

### 2b. Install Missing Dependencies (5 mins)
```bash
cd /Users/udaymukhija/gathr/frontend
npm install react-native-maps@1.8.0
npx expo install --check
```

### 2c. Test on Expo Go (5 mins)
```bash
npm start
# Scan QR code with Expo Go app on your phone
```

**Try the flow:**
1. Enter phone number → Get OTP
2. Enter "123456" (mock mode) → Login
3. Complete onboarding (pick SPORTS, FOOD)
4. See personalized feed!

✅ **App works with production backend!**

---

## STEP 3: Build Android APK (60 minutes)

### 3a. Configure EAS (10 mins)
```bash
cd /Users/udaymukhija/gathr/frontend
eas login
eas init
```

Create `/Users/udaymukhija/gathr/frontend/eas.json`:
```json
{
  "build": {
    "preview": {
      "android": {
        "buildType": "apk",
        "env": {
          "EXPO_PUBLIC_API_URL": "https://gathr-backend-production.up.railway.app"
        }
      }
    },
    "production": {
      "android": {
        "buildType": "app-bundle"
      }
    }
  }
}
```

### 3b. Update app.json (5 mins)
Edit `/Users/udaymukhija/gathr/frontend/app.json`:
```json
{
  "expo": {
    "name": "Gathr",
    "slug": "gathr",
    "version": "1.0.0",
    "android": {
      "package": "com.gathr.app",
      "versionCode": 1,
      "adaptiveIcon": {
        "foregroundImage": "./assets/adaptive-icon.png",
        "backgroundColor": "#6200EE"
      },
      "config": {
        "googleMaps": {
          "apiKey": "YOUR_GOOGLE_MAPS_API_KEY"
        }
      }
    }
  }
}
```

**Note:** For map view, get Google Maps API key:
- Go to https://console.cloud.google.com
- Enable "Maps SDK for Android"
- Create API key
- Paste in app.json above

### 3c. Build APK (45 mins)
```bash
eas build --platform android --profile preview
# EAS will build in cloud (~30-45 minutes)
```

When done, you'll get a download link like:
`https://expo.dev/artifacts/eas/builds/abc123.apk`

### 3d. Share with Testers (1 min)
```bash
# Copy APK link and send to friends:
# "Download Gathr: https://expo.dev/artifacts/eas/builds/abc123.apk"
```

✅ **You have a shareable APK!**

---

## STEP 4: Test with Real Users (60 minutes)

### 4a. Create Test Activities (10 mins)
- Open app on your phone
- Create 3-5 activities in different categories (SPORTS, FOOD, ART)
- Set times for today/tomorrow

### 4b. Invite 5-10 Friends (10 mins)
- Share APK link
- Ask them to:
  1. Install APK
  2. Sign up with phone
  3. Pick 2-3 interests during onboarding
  4. Browse feed and join activities

### 4c. Watch Feed Personalization Work (20 mins)
Have friends share screenshots of their feeds. You'll see:
- Person who picked SPORTS sees basketball first
- Person who picked FOOD sees coffee meetup first
- Everyone sees different ordering based on interests!

### 4d. Monitor Backend (20 mins)
```bash
railway logs --follow
# Watch for:
# - "FeedScoringEngine" logs showing scoring calculations
# - "Activity joined" events
# - Any errors
```

✅ **You have 10 people using your app!**

---

## WHAT YOU'VE LAUNCHED

🎉 **Congratulations! You just launched:**
- Personalized activity discovery app
- Interest-based recommendation engine
- Real-time chat for activities
- Safety features (block, report, trust score)
- Privacy-preserving contact matching

**All running on production infrastructure with real users.**

---

## Next Steps (After Launch)

### Week 1: Monitor & Fix Bugs
- Watch Railway logs for errors
- Fix any crashes reported by testers
- Adjust scoring weights based on feedback

### Week 2: Add Polish
- Switch to real Twilio SMS (set `OTP_PROVIDER=twilio`)
- Enable push notifications (Firebase setup)
- Add map view for visual discovery

### Week 3: Growth
- Invite 50 more users
- Create promotional activities
- Track metrics (sign-ups, joins, retention)

---

## Common Issues & Fixes

### "Backend won't start on Railway"
```bash
# Check logs:
railway logs --tail 100

# Common fix: Increase memory
railway service update --memory 2048
```

### "APK won't install on Android"
- Enable "Install from unknown sources" in Android settings
- Or share via TestFlight/Google Play internal testing

### "Feed shows no activities"
- Create test activities first (you need data!)
- Check backend logs: `railway logs | grep FeedService`

### "OTP not working"
- Mock mode: Use ANY 6-digit code (123456 works)
- For real Twilio: Set `TWILIO_*` env vars and change `OTP_PROVIDER=twilio`

---

## Emergency Rollback

If something breaks:
```bash
# Railway auto-saves previous deployments
railway status
railway rollback  # Goes back to last working version
```

---

## You Did It! 🎉

**Before:** Local dev setup, never deployed
**After:** Live production app with real users

**Time:** 4 hours
**Cost:** $0 (Railway free tier)
**Users:** 10+ alpha testers

**What's Next:** Keep building, keep launching. You're a founder now.
