# SMS/OTP Integration Guide

## Overview

Gathr uses a flexible OTP (One-Time Password) system that supports both development (mock) and production (Twilio) modes.

## Architecture

### Components

1. **OtpService Interface** (`com.gathr.service.OtpService`)
   - Defines the contract for OTP operations
   - Methods: `sendOtp()`, `verifyOtp()`, `clearOtp()`

2. **MockOtpServiceImpl** (Development)
   - Always uses OTP: `123456`
   - Logs OTP to console
   - Active when `otp.provider=mock` (default)

3. **TwilioOtpServiceImpl** (Production)
   - Sends real SMS via Twilio
   - Generates random 6-digit OTPs
   - Active when `otp.provider=twilio`

4. **AuthService**
   - Handles authentication flow
   - Uses OtpService for sending and verifying OTPs
   - Implements rate limiting (3 OTPs per hour per phone)

## Development Setup

### Default Configuration (Mock Mode)

No setup required! The app defaults to mock mode:

```properties
# application.properties (default)
otp.provider=mock
```

Test OTP for any phone number: **123456**

### Testing

1. Start the backend: `mvn spring-boot:run`
2. Request OTP: `POST /auth/otp` with `{"phone": "+1234567890"}`
3. Check logs for OTP (always "123456" in mock mode)
4. Verify OTP: `POST /auth/verify` with `{"phone": "+1234567890", "otp": "123456"}`

## Production Setup (Twilio)

### 1. Create Twilio Account

1. Sign up at https://www.twilio.com/
2. Get a phone number capable of sending SMS
3. Get your credentials from https://www.twilio.com/console:
   - Account SID
   - Auth Token
   - Phone Number

### 2. Configure Environment Variables

```bash
export OTP_PROVIDER=twilio
export TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_AUTH_TOKEN=your_auth_token_here
export TWILIO_PHONE_NUMBER=+15551234567
```

### 3. Update application.properties (or use env vars)

```properties
# application.properties
otp.provider=twilio
twilio.account-sid=${TWILIO_ACCOUNT_SID}
twilio.auth-token=${TWILIO_AUTH_TOKEN}
twilio.phone-number=${TWILIO_PHONE_NUMBER}
```

### 4. Deploy and Test

1. Deploy with production profile:
   ```bash
   java -jar -Dspring.profiles.active=prod gathr-backend.jar
   ```

2. Test with a real phone number:
   ```bash
   curl -X POST http://your-api/auth/otp \
     -H "Content-Type: application/json" \
     -d '{"phone": "+15551234567"}'
   ```

3. You should receive an SMS: "Your Gathr verification code is: 123456. This code will expire in 5 minutes."

## Phone Number Format

Always use E.164 format:
- ✅ Correct: `+14155552671`
- ❌ Wrong: `4155552671`
- ❌ Wrong: `(415) 555-2671`

## OTP Configuration

### Defaults

- **OTP Length**: 6 digits
- **Expiry Time**: 5 minutes
- **Rate Limit**: 3 requests per hour per phone number
- **Storage**: In-memory (ConcurrentHashMap)

### Production Recommendations

For production with multiple instances, replace in-memory storage with Redis:

```java
// TODO: Replace ConcurrentHashMap with Redis
// Example:
@Autowired
private RedisTemplate<String, OtpEntry> redisTemplate;

public void storeOtp(String phone, String otp) {
    redisTemplate.opsForValue().set(
        "otp:" + phone,
        new OtpEntry(otp, expiryTime),
        5, TimeUnit.MINUTES
    );
}
```

## Security Considerations

1. **Rate Limiting**: Currently in-memory. Use Redis for distributed rate limiting.
2. **OTP Brute Force**: Consider adding exponential backoff after failed attempts.
3. **Phone Verification**: Twilio automatically validates phone number format.
4. **Secrets Management**: Never commit `twilio.auth-token` to version control.
5. **HTTPS**: Always use HTTPS in production to protect OTP transmission.

## Troubleshooting

### "Twilio credentials not configured" error

**Cause**: Missing or empty Twilio credentials.

**Fix**: Set environment variables or update application.properties:
```bash
export TWILIO_ACCOUNT_SID=ACxxxxxx
export TWILIO_AUTH_TOKEN=xxxxxx
export TWILIO_PHONE_NUMBER=+1234567890
```

### "Failed to send OTP via Twilio" error

**Possible causes**:
1. Invalid phone number format (must be E.164)
2. Twilio account not verified (trial accounts can only send to verified numbers)
3. Insufficient Twilio balance
4. Phone number not purchased or not SMS-capable

**Fix**: Check Twilio console logs at https://www.twilio.com/console/sms/logs

### "Too many OTP requests" error

**Cause**: Rate limit exceeded (3 requests per hour per phone).

**Fix**: Wait 1 hour or clear rate limit (dev only):
```java
// In development, you can clear rate limiting in AuthService
otpRequestHistory.clear();
```

## Cost Estimation

### Twilio Pricing (as of 2024)

- **SMS to US**: $0.0079 per message
- **SMS to International**: Varies by country ($0.05-$0.30 per message)

### Example Costs

Assuming 10,000 users per month:
- **US only**: 10,000 × $0.0079 = **$79/month**
- **Mixed international**: ~**$150-200/month**

### Cost Optimization

1. Use SMS only for verification (not for notifications)
2. Implement OTP resend cooldown (e.g., 60 seconds)
3. Monitor and alert on unusual OTP request patterns
4. Consider free alternatives like Firebase Phone Auth for mobile apps

## Alternative SMS Providers

To switch from Twilio to another provider:

1. Create new implementation of `OtpService`:
   ```java
   @Service
   @ConditionalOnProperty(name = "otp.provider", havingValue = "aws-sns")
   public class AwsSnsOtpServiceImpl implements OtpService {
       // Implementation using AWS SNS
   }
   ```

2. Update configuration:
   ```properties
   otp.provider=aws-sns
   ```

Supported alternatives:
- AWS SNS (Simple Notification Service)
- Firebase Cloud Messaging
- MessageBird
- Nexmo/Vonage

## Testing Checklist

- [ ] Mock mode works in development
- [ ] Real SMS delivery works in production
- [ ] OTP expiry works (5 minutes)
- [ ] Rate limiting works (3 per hour)
- [ ] Invalid OTP is rejected
- [ ] Expired OTP is rejected
- [ ] Phone number validation works
- [ ] Twilio credentials are not committed to repo
- [ ] Production uses environment variables for secrets

## Support

For issues with:
- **Gathr OTP integration**: Check application logs
- **Twilio service**: https://www.twilio.com/docs/sms
- **Phone number formatting**: https://www.twilio.com/docs/glossary/what-e164

## References

- [Twilio SMS Quickstart](https://www.twilio.com/docs/sms/quickstart/java)
- [E.164 Phone Number Format](https://www.twilio.com/docs/glossary/what-e164)
- [Twilio Console](https://www.twilio.com/console)
