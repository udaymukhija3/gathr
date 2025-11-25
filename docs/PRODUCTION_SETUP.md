# Production Setup Guide

This document outlines the environment variables and configuration needed for production deployment.

## Environment Variables

### Required for Production

```bash
# Database
DB_HOST=your-postgres-host.com
DB_PORT=5432
DB_NAME=gathr_production
DB_USERNAME=your_db_user
DB_PASSWORD=your_secure_db_password

# Security
JWT_SECRET=your-256-bit-secret-key-minimum-32-characters

# SMS (Twilio)
OTP_PROVIDER=twilio
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890

# Push Notifications (Firebase)
PUSH_PROVIDER=firebase
FIREBASE_SERVER_KEY=your_fcm_server_key

# CORS
CORS_ALLOWED_ORIGINS=https://your-app-domain.com
CORS_ALLOW_CREDENTIALS=true

# App URL (for invite links)
APP_URL=https://gathr.app
```

### Optional

```bash
# Slack Webhook (for report notifications)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx/yyy/zzz

# OTP Configuration
OTP_EXPIRY_MINUTES=5

# Activity Configuration
ACTIVITY_IDENTITY_REVEAL_THRESHOLD=3
ACTIVITY_DEFAULT_MAX_MEMBERS=4

# Invite Token Configuration
INVITE_TOKEN_EXPIRY_HOURS=48
```

## Twilio Setup

1. **Create a Twilio Account**: Go to [twilio.com](https://www.twilio.com) and sign up
2. **Get Your Credentials**:
   - Account SID: Found on your Twilio Console Dashboard
   - Auth Token: Found on your Twilio Console Dashboard
3. **Get a Phone Number**:
   - Go to Phone Numbers → Manage → Buy a Number
   - Choose a number with SMS capability
   - For India, you may need to register for A2P 10DLC or use a short code
4. **Verify Your Account**:
   - For trial accounts, you can only send SMS to verified numbers
   - Upgrade to a paid account to send to any number

### India-Specific Requirements

If targeting India, you'll need:
- DLT (Distributed Ledger Technology) registration
- Sender ID registration
- Template registration for transactional SMS

Consider using Twilio's [India-specific guides](https://www.twilio.com/docs/messaging/getting-started/india-messaging-guidelines).

## Firebase Setup

1. **Create a Firebase Project**: Go to [Firebase Console](https://console.firebase.google.com)
2. **Enable Cloud Messaging**:
   - Go to Project Settings → Cloud Messaging
   - Enable Firebase Cloud Messaging API (V1)
3. **Get Server Key**:
   - Under Cloud Messaging, find the "Server key"
   - Copy this value for `FIREBASE_SERVER_KEY`
4. **Add App to Firebase**:
   - Register your iOS and Android apps
   - Download and add the google-services.json (Android) or GoogleService-Info.plist (iOS)

## Expo Push Notifications Setup

For Expo-based apps:

1. **Get Expo Push Token**: The app uses `expo-notifications` to get device tokens
2. **Configure EAS**:
   - Run `eas credentials` to set up push notification credentials
   - For iOS, EAS will handle APNs configuration
   - For Android, add your FCM credentials to EAS

## Database Migrations

Before deploying, ensure migrations are applied:

```bash
# Flyway will auto-migrate on startup, but you can also run manually:
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} \
                      -Dflyway.user=${DB_USERNAME} \
                      -Dflyway.password=${DB_PASSWORD}
```

## Health Check

After deployment, verify the backend is running:

```bash
curl https://your-backend-url.com/actuator/health
```

## Switching from Mock to Production

In development, the app uses mock services. To switch to production:

1. Set `OTP_PROVIDER=twilio` (instead of `mock`)
2. Set `PUSH_PROVIDER=firebase` (instead of `mock`)
3. Set `EXPO_PUBLIC_MOCK_MODE=false` in the frontend environment

## Security Checklist

- [ ] JWT_SECRET is at least 256 bits (32 characters)
- [ ] Database password is strong and unique
- [ ] Twilio credentials are not committed to version control
- [ ] Firebase credentials are not committed to version control
- [ ] CORS is configured for your specific domain only
- [ ] HTTPS is enforced on all endpoints
- [ ] Rate limiting is enabled on sensitive endpoints (OTP, etc.)
