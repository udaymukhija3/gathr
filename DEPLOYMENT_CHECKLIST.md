# ✅ Deployment Checklist - GO/NO-GO for Launch

## Pre-Deployment Health Check

Run these commands to verify everything works locally:

### Backend Health Check
```bash
cd /Users/udaymukhija/gathr

# 1. Compile succeeds
mvn clean compile
# ✅ Should see "BUILD SUCCESS"

# 2. Start backend locally
mvn spring-boot:run &
sleep 30  # Wait for startup

# 3. Test health endpoint
curl http://localhost:8080/actuator/health
# ✅ Should return {"status":"UP"}

# 4. Test OTP flow
curl -X POST http://localhost:8080/auth/otp/start \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919999999999"}'
# ✅ Should return 200 OK

# 5. Kill backend
pkill -f "spring-boot:run"
```

### Frontend Health Check
```bash
cd /Users/udaymukhija/gathr/frontend

# 1. Install dependencies
npm install
# ✅ Should complete without errors

# 2. Start Expo dev server
npm start &
sleep 10

# 3. Check it's running
curl http://localhost:8081
# ✅ Should see "React Native packager is running"

# 4. Kill Expo
pkill -f "expo start"
```

---

## Railway Deployment Checklist

### Step 1: Install Railway CLI
- [ ] `brew install railway` (Mac)
- [ ] `railway login` succeeds
- [ ] Railway account has payment method (for production usage)

### Step 2: Create Project
- [ ] `railway init` creates new project
- [ ] Project named "gathr-backend"
- [ ] `railway add postgresql` adds database
- [ ] `railway variables` shows `DATABASE_URL`

### Step 3: Set Environment Variables
Copy-paste this block:
```bash
railway variables set JWT_SECRET="gathr-production-secret-key-change-this-minimum-256-bits-long-string-for-security"
railway variables set OTP_PROVIDER="mock"
railway variables set CORS_ALLOWED_ORIGINS="*"
railway variables set APP_URL="https://gathr.app"
railway variables set SPRING_PROFILES_ACTIVE="prod"
railway variables set SPRING_JPA_HIBERNATE_DDL_AUTO="validate"
railway variables set SPRING_FLYWAY_ENABLED="true"
```

Verify:
- [ ] `railway variables` shows all 7 variables

### Step 4: Deploy Backend
- [ ] `railway.json` exists in project root
- [ ] `railway up` starts deployment
- [ ] Wait 10-15 minutes for Maven build
- [ ] `railway logs --tail 50` shows "Started GathrApplication"
- [ ] No red ERROR lines in logs

### Step 5: Get Backend URL
```bash
railway domain
```
- [ ] Copy URL (e.g., `https://gathr-backend-production.up.railway.app`)
- [ ] Paste into `/Users/udaymukhija/gathr/frontend/src/services/api.ts` line 25

### Step 6: Test Deployed Backend
Replace `$BACKEND_URL` with your Railway URL:
```bash
BACKEND_URL="https://your-app.up.railway.app"

# Health check
curl $BACKEND_URL/actuator/health
# ✅ {"status":"UP"}

# OTP start
curl -X POST $BACKEND_URL/auth/otp/start \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919999999999"}'
# ✅ 200 OK

# OTP verify
curl -X POST $BACKEND_URL/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone":"+919999999999","otp":"123456"}'
# ✅ Returns JWT token
```

Checklist:
- [ ] Health check returns UP
- [ ] OTP start succeeds
- [ ] OTP verify returns token
- [ ] No 500 errors

---

## Expo/EAS Build Checklist

### Step 1: Configure EAS
- [ ] `npm install -g eas-cli`
- [ ] `eas login` with Expo account
- [ ] `eas init` in frontend directory
- [ ] `eas.json` created

### Step 2: Update API URL
In `/Users/udaymukhija/gathr/frontend/src/services/api.ts`:
- [ ] Line 25 updated with Railway backend URL
- [ ] Or set `EXPO_PUBLIC_API_URL` in `eas.json`

### Step 3: Get Google Maps API Key (for MapView)
- [ ] Go to https://console.cloud.google.com
- [ ] Create new project "Gathr"
- [ ] Enable "Maps SDK for Android"
- [ ] Create API key (restrict to Android apps)
- [ ] Paste key into `app.json` android.config.googleMaps.apiKey
- [ ] Key looks like: `AIzaSyC...` (39 characters)

### Step 4: Build APK
```bash
cd /Users/udaymukhija/gathr/frontend
eas build --platform android --profile preview
```

Checklist:
- [ ] Build queued on Expo servers
- [ ] Wait 30-45 minutes
- [ ] Build succeeds (check https://expo.dev)
- [ ] Download APK link received
- [ ] APK downloads (check size >30MB)

### Step 5: Test APK on Device
- [ ] Install APK on Android phone
- [ ] App opens without crashing
- [ ] Sign up with phone number works
- [ ] Onboarding completes (select interests)
- [ ] Feed loads (may be empty if no activities)
- [ ] Can create activity
- [ ] Can view activity details

---

## Alpha Testing Checklist

### Step 1: Create Test Data
- [ ] Log into app yourself
- [ ] Create 5-10 activities across different categories:
  - [ ] 2 SPORTS activities (basketball, cricket)
  - [ ] 2 FOOD activities (coffee, dinner)
  - [ ] 2 ART activities (painting, museum)
  - [ ] 1-2 MUSIC activities (concert, jam session)
  - [ ] 1-2 OUTDOOR activities (hike, park)
- [ ] Set times for today/tomorrow
- [ ] Set different hubs/locations

### Step 2: Invite Testers (5-10 people)
Send them:
```
Hey! I just launched Gathr - an app to find spontaneous local activities based on your interests.

Download: [APK link from EAS]

After you sign up:
1. Pick 2-3 interests during onboarding
2. Check the feed - it'll be personalized for YOU
3. Join an activity and let me know what you think!

Looking for feedback on the recommendation engine - does it show you relevant stuff?
```

Checklist:
- [ ] At least 5 people install
- [ ] At least 3 complete onboarding
- [ ] At least 2 join an activity

### Step 3: Monitor First 24 Hours
```bash
# Watch backend logs
railway logs --follow | grep -E "FeedService|joined|error"
```

Track:
- [ ] Sign-ups (check database: `SELECT COUNT(*) FROM users`)
- [ ] Activity joins (check: `SELECT COUNT(*) FROM participations`)
- [ ] Any errors (check logs)
- [ ] Feedback from testers

---

## Production Readiness Checklist

### Must Have (Go/No-Go)
- [ ] Backend deployed and accessible
- [ ] Database running with migrations applied
- [ ] Frontend builds successfully
- [ ] Can complete full user flow (sign up → onboarding → feed → join)
- [ ] No critical bugs blocking usage

### Nice to Have (Can add post-launch)
- [ ] Real Twilio SMS (vs mock OTP)
- [ ] Push notifications working
- [ ] Map view showing activities
- [ ] Analytics tracking
- [ ] Monitoring/alerts

### Known Limitations (Document for testers)
- [ ] Mock OTP (any 6-digit code works)
- [ ] No push notifications yet
- [ ] Map view not yet implemented
- [ ] Limited to test users only

---

## Rollback Plan

If deployment fails:

### Backend Issues
```bash
# Check logs
railway logs --tail 100

# Rollback to previous version
railway rollback

# Or redeploy
git reset --hard HEAD~1
railway up
```

### Frontend Issues
- Previous APK still works if backend is stable
- Fix bug → Rebuild APK → Share new link

### Database Issues
```bash
# Connect to Railway DB
railway psql

# Check migrations
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

# Rollback migration if needed
DELETE FROM flyway_schema_history WHERE installed_rank = (SELECT MAX(installed_rank) FROM flyway_schema_history);
```

---

## Success Criteria

### Day 1 (Launch Day)
- [ ] 5+ users signed up
- [ ] 2+ activities created
- [ ] 1+ activity with multiple people joined
- [ ] Zero critical bugs reported

### Week 1
- [ ] 20+ users signed up
- [ ] 10+ activities created
- [ ] 5+ completed activities with feedback
- [ ] Personalization working (users see relevant activities first)

### Month 1
- [ ] 100+ users
- [ ] 50+ activities created
- [ ] >30% weekly retention
- [ ] Switch to real Twilio OTP
- [ ] Push notifications enabled
- [ ] Map view launched

---

## Emergency Contacts

### If Something Breaks

**Railway Support:**
- Docs: https://docs.railway.app
- Discord: https://discord.gg/railway
- Status: https://status.railway.app

**Expo Support:**
- Docs: https://docs.expo.dev
- Forums: https://forums.expo.dev
- Status: https://status.expo.dev

**Your Backend Logs:**
```bash
railway logs --follow
```

**Your Database:**
```bash
railway psql
```

---

## Final Go/No-Go Decision

**READY TO LAUNCH IF:**
- [x] Backend health check passes
- [x] Frontend builds successfully
- [x] Can complete full user flow
- [x] At least 3 friends committed to testing
- [x] Rollback plan understood

**WAIT IF:**
- [ ] Backend doesn't start on Railway
- [ ] Frontend crashes on launch
- [ ] Database migrations fail
- [ ] Critical bugs in auth/onboarding
- [ ] Zero test users available

---

## You're Ready! 🚀

If all checks above pass, you have a **production-ready MVP**. The recommendation engine works, the feed is personalized, the core flows are solid.

**Launch now. Iterate fast. You've got this.**
