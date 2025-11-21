# Security Headers Documentation

## Overview

Gathr implements comprehensive security headers to protect against common web vulnerabilities. This document explains each header, its purpose, and how it's configured.

## Security Headers Implemented

### 1. X-Frame-Options: DENY

**Purpose**: Prevents clickjacking attacks

**Protection**:
- Prevents the application from being embedded in `<iframe>`, `<frame>`, `<embed>`, or `<object>` tags
- Protects users from clicking on hidden elements overlaid on legitimate UI

**Configuration**:
```java
.frameOptions(frameOptions -> frameOptions.deny())
```

**Header Sent**:
```
X-Frame-Options: DENY
```

### 2. X-Content-Type-Options: nosniff

**Purpose**: Prevents MIME-sniffing attacks

**Protection**:
- Forces browsers to respect the `Content-Type` header
- Prevents browsers from interpreting files as a different MIME type
- Blocks execution of JavaScript if served with wrong Content-Type

**Configuration**:
```java
.contentTypeOptions(contentTypeOptions -> {})
```

**Header Sent**:
```
X-Content-Type-Options: nosniff
```

### 3. X-XSS-Protection: 1; mode=block

**Purpose**: Enables XSS filtering in older browsers

**Protection**:
- Enables built-in XSS filter in Internet Explorer, Chrome, and Safari
- Blocks page rendering if XSS attack is detected
- Note: Modern browsers rely on CSP instead

**Configuration**:
```java
.xssProtection(xss -> xss.headerValue("1; mode=block"))
```

**Header Sent**:
```
X-XSS-Protection: 1; mode=block
```

### 4. Content-Security-Policy (CSP)

**Purpose**: Prevents XSS, data injection, and other code injection attacks

**Protection**:
- Defines which resources can be loaded and from where
- Blocks inline scripts and eval() by default
- Restricts where scripts, styles, images, and fonts can be loaded from

**Configuration**:
```java
.contentSecurityPolicy(csp -> csp
    .policyDirectives("default-src 'self'; " +
        "script-src 'self' 'unsafe-inline'; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data: https:; " +
        "font-src 'self' data:; " +
        "connect-src 'self' ws: wss:; " +
        "frame-ancestors 'none'")
)
```

**Header Sent**:
```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' ws: wss:; frame-ancestors 'none'
```

**Policy Breakdown**:
- `default-src 'self'`: Only load resources from same origin by default
- `script-src 'self' 'unsafe-inline'`: Scripts from same origin + inline scripts
  - **Note**: `'unsafe-inline'` is needed for React Native web builds
  - Consider using nonces in production for better security
- `style-src 'self' 'unsafe-inline'`: Styles from same origin + inline styles
- `img-src 'self' data: https:`: Images from same origin, data URIs, and HTTPS URLs
- `font-src 'self' data:`: Fonts from same origin and data URIs
- `connect-src 'self' ws: wss:`: API calls to same origin, WebSocket connections
- `frame-ancestors 'none'`: Cannot be embedded in iframes (same as X-Frame-Options: DENY)

### 5. Strict-Transport-Security (HSTS) - Production Only

**Purpose**: Forces HTTPS connections

**Protection**:
- Forces browsers to only connect via HTTPS
- Prevents downgrade attacks and cookie hijacking
- Includes subdomains

**Activation**: Only enabled when `spring.profiles.active=prod`

**Configuration**:
```java
response.setHeader("Strict-Transport-Security",
    "max-age=31536000; includeSubDomains; preload");
```

**Header Sent** (Production only):
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

**IMPORTANT**:
- Only enable HSTS when serving over HTTPS
- `max-age=31536000` = 1 year
- `includeSubDomains`: Apply to all subdomains
- `preload`: Eligible for browser HSTS preload lists

### 6. Referrer-Policy - Production Only

**Purpose**: Controls how much referrer information is sent with requests

**Protection**:
- Prevents leaking sensitive URLs to third parties
- Balances security with analytics needs

**Activation**: Only enabled when `spring.profiles.active=prod`

**Configuration**:
```java
response.setHeader("Referrer-Policy",
    "strict-origin-when-cross-origin");
```

**Header Sent** (Production only):
```
Referrer-Policy: strict-origin-when-cross-origin
```

**Policy Meaning**:
- Same-origin requests: Send full URL
- Cross-origin requests: Send only origin (no path)
- HTTPS → HTTP: Don't send referrer

### 7. Permissions-Policy - Production Only

**Purpose**: Controls which browser features can be used

**Protection**:
- Disables unused browser features to reduce attack surface
- Prevents malicious scripts from accessing camera, mic, geolocation

**Activation**: Only enabled when `spring.profiles.active=prod`

**Configuration**:
```java
response.setHeader("Permissions-Policy",
    "geolocation=(), microphone=(), camera=()");
```

**Header Sent** (Production only):
```
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

**Features Disabled**:
- `geolocation=()`: No access to geolocation
- `microphone=()`: No access to microphone
- `camera=()`: No access to camera

## How to Test Security Headers

### Method 1: Using cURL

```bash
# Test development environment
curl -I http://localhost:8080/health

# Expected headers:
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
# X-XSS-Protection: 1; mode=block
# Content-Security-Policy: default-src 'self'; ...

# Test production environment
curl -I https://api.gathr.com/health

# Additional expected headers in production:
# Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
# Referrer-Policy: strict-origin-when-cross-origin
# Permissions-Policy: geolocation=(), microphone=(), camera=()
```

### Method 2: Using Browser DevTools

1. Open Chrome DevTools (F12)
2. Go to Network tab
3. Make a request to the API
4. Click on the request
5. Go to Headers tab
6. Look for "Response Headers"

### Method 3: Online Security Scanners

- [SecurityHeaders.com](https://securityheaders.com/)
- [Mozilla Observatory](https://observatory.mozilla.org/)
- [OWASP ZAP](https://www.zaproxy.org/)

## Environment Configuration

### Development

Security headers are active by default, except HSTS:

```properties
# application.properties (default)
# All headers except HSTS are enabled
```

### Production

Enable production security profile:

```bash
# Via command line
java -jar -Dspring.profiles.active=prod gathr-backend.jar

# Via environment variable
export SPRING_PROFILES_ACTIVE=prod

# Via Kubernetes/Docker
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
```

## Security Header Checklist

Before going to production, verify:

- [ ] All security headers present in development
- [ ] HSTS only enabled in production (HTTPS required)
- [ ] CSP policy doesn't break frontend functionality
- [ ] Test with SecurityHeaders.com (should get A+ rating)
- [ ] Verify no mixed content warnings in browser console
- [ ] Test WebSocket connections work with CSP
- [ ] Verify API calls work across CORS origins
- [ ] Check that inline scripts work (or add nonces)

## Troubleshooting

### CSP Blocking Resources

**Symptom**: Console errors like "Refused to load... violates CSP directive"

**Solution**: Update CSP policy to allow the resource source

```java
// Example: Allow images from CDN
"img-src 'self' data: https: https://cdn.example.com;"
```

### HSTS Certificate Errors

**Symptom**: Browser shows "Your connection is not private" error

**Solution**:
1. Ensure HTTPS is properly configured
2. Check SSL certificate is valid
3. Only enable HSTS after HTTPS is working
4. Use `max-age=300` (5 minutes) initially for testing

### WebSocket Connection Blocked

**Symptom**: WebSocket connections fail with CSP errors

**Solution**: Ensure CSP includes `connect-src 'self' ws: wss:`

### Inline Scripts Blocked

**Symptom**: React Native web build shows blank page

**Solution**:
- Add `'unsafe-inline'` to `script-src` (less secure)
- OR use CSP nonces (more secure):
  ```java
  "script-src 'self' 'nonce-" + generateNonce() + "';"
  ```

## Attack Scenarios Prevented

### 1. Clickjacking (X-Frame-Options)

**Attack**: Attacker embeds your app in an invisible iframe and tricks users into clicking

**Prevention**: `X-Frame-Options: DENY` prevents embedding

### 2. MIME Sniffing (X-Content-Type-Options)

**Attack**: Attacker uploads SVG file, browser interprets it as HTML and executes script

**Prevention**: `X-Content-Type-Options: nosniff` forces correct MIME type

### 3. XSS (Content-Security-Policy)

**Attack**: Attacker injects `<script>` tag via user input

**Prevention**: CSP blocks inline scripts and restricts script sources

### 4. Man-in-the-Middle (HSTS)

**Attack**: Attacker downgrades HTTPS to HTTP and intercepts traffic

**Prevention**: HSTS forces HTTPS only, preventing downgrade

### 5. Referer Leakage (Referrer-Policy)

**Attack**: Sensitive URL parameters leaked to third-party analytics

**Prevention**: Referrer-Policy limits what's sent in Referer header

## References

- [OWASP Secure Headers Project](https://owasp.org/www-project-secure-headers/)
- [MDN Security Headers](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers#security)
- [Spring Security Headers](https://docs.spring.io/spring-security/reference/features/exploits/headers.html)
- [Content-Security-Policy.com](https://content-security-policy.com/)
