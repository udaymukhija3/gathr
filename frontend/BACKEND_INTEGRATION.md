# Backend Integration Checklist

This document lists all backend endpoints and their expected JSON schemas that the frontend expects.

## Required Backend Endpoints

### 1. Authentication

#### POST /auth/otp/start
**Request:**
```json
{
  "phone": "1234567890"
}
```

**Response (200):**
```json
{
  "message": "OTP sent successfully"
}
```

**Response (429 - Rate Limited):**
```json
{
  "error": "Too many OTP requests. Please try again later."
}
```

#### POST /auth/otp/verify
**Request:**
```json
{
  "phone": "1234567890",
  "otp": "123456"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "name": "John Doe",
    "phone": "1234567890",
    "verified": true,
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

---

### 2. Hubs

#### GET /hubs
**Response (200):**
```json
[
  {
    "id": 1,
    "name": "Cyberhub",
    "area": "Cyber City",
    "description": "A bustling hub..."
  }
]
```

---

### 3. Activities

#### GET /activities?hub_id=1&date=2024-12-31
**Response (200):**
```json
[
  {
    "id": 1,
    "title": "Coffee at Galleria",
    "hubId": 2,
    "hubName": "Galleria",
    "category": "FOOD",
    "startTime": "2024-12-31T18:30:00+05:30",
    "endTime": "2024-12-31T20:00:00+05:30",
    "createdBy": 1,
    "createdByName": "Creator",
    "interestedCount": 3,
    "confirmedCount": 2,
    "totalParticipants": 5,
    "peopleCount": 5,
    "mutualsCount": 2,
    "isInviteOnly": false,
    "revealIdentities": false,
    "maxMembers": 8
  }
]
```

#### GET /activities?latitude=28.45&longitude=77.03&radiusKm=5
**Response (200):**
```json
[
  {
    "id": 42,
    "title": "Pickleball at Ridgewood",
    "locationName": "Ridgewood Courts",
    "locationAddress": "Sec 27, Gurugram",
    "latitude": 28.4471,
    "longitude": 77.0321,
    "distanceKm": 1.2,
    "category": "SPORTS",
    "startTime": "2024-12-31T18:30:00+05:30",
    "endTime": "2024-12-31T20:00:00+05:30",
    "createdBy": 7,
    "createdByName": "Creator",
    "peopleCount": 4,
    "isInviteOnly": false,
    "maxMembers": 8
  }
]
```

#### GET /activities/:id
**Response (200):**
```json
{
  "id": 1,
  "title": "Coffee at Galleria",
  "hubId": 2,
  "hubName": "Galleria",
  "category": "FOOD",
  "startTime": "2024-12-31T18:30:00+05:30",
  "endTime": "2024-12-31T20:00:00+05:30",
  "createdBy": 1,
  "createdByName": "Creator",
  "interestedCount": 3,
  "confirmedCount": 2,
  "totalParticipants": 5,
  "peopleCount": 5,
  "mutualsCount": 2,
  "isInviteOnly": false,
  "revealIdentities": false,
  "maxMembers": 8,
  "participants": [
    {
      "userId": 1,
      "mutualsCount": 2,
      "revealed": false
    },
    {
      "userId": 2,
      "firstName": "John",
      "mutualsCount": 1,
      "revealed": true
    }
  ]
}
```

#### POST /activities
**Request (curated hub):**
```json
{
  "title": "Coffee Tonight",
  "hubId": 1,
  "category": "FOOD",
  "startTime": "2024-12-31T18:00:00+05:30",
  "endTime": "2024-12-31T20:00:00+05:30",
  "isInviteOnly": false,
  "maxMembers": 4
}
```

**Request (custom location):**
```json
{
  "title": "Pickleball at Ridgewood",
  "category": "SPORTS",
  "startTime": "2024-12-31T18:00:00+05:30",
  "endTime": "2024-12-31T20:00:00+05:30",
  "placeName": "Ridgewood Courts",
  "placeAddress": "Sec 27, Gurugram",
  "latitude": 28.4471,
  "longitude": 77.0321,
  "placeId": "mapbox.123",
  "isInviteOnly": false,
  "maxMembers": 8
}
```

**Response (200):**
```json
{
  "id": 123,
  "title": "Coffee Tonight",
  "hubId": 1,
  "hubName": "Cyberhub",
  "category": "FOOD",
  "startTime": "2024-12-31T18:00:00+05:30",
  "endTime": "2024-12-31T20:00:00+05:30",
  "createdBy": 1,
  "createdByName": "Current User",
  "peopleCount": 1,
  "mutualsCount": 0,
  "isInviteOnly": false,
  "revealIdentities": false,
  "maxMembers": 4
}
```

#### POST /activities/:id/join?status=INTERESTED&inviteToken=<token>
**Response (200):**
```json
{
  "message": "Successfully joined activity"
}
```

**Response (403 - Invite Token Required):**
```json
{
  "error": "Invite token required for invite-only activities"
}
```

**Response (403 - Invalid Token):**
```json
{
  "error": "Invalid or expired invite token"
}
```

**Response (409 - Full):**
```json
{
  "error": "Activity has reached maximum number of participants"
}
```

#### POST /activities/:id/invite-token
**Response (200):**
```json
{
  "token": "abc123def456...",
  "expiresAt": "2025-01-02T18:00:00+05:30"
}
```

#### POST /activities/:id/confirm
**Request:** (no body, JWT in header)
**Response (200):**
```json
{
  "message": "Participation confirmed"
}
```

---

### 4. Messages

#### GET /activities/:id/messages?since=2024-12-31T18:00:00Z
**Response (200):**
```json
[
  {
    "id": 1,
    "activityId": 1,
    "userId": 1,
    "userName": "John",
    "text": "Hello!",
    "createdAt": "2024-12-31T18:05:00+05:30"
  }
]
```

#### POST /activities/:id/messages
**Request:**
```json
{
  "text": "Hello everyone!"
}
```

**Response (200):**
```json
{
  "id": 123,
  "activityId": 1,
  "userId": 1,
  "userName": "You",
  "text": "Hello everyone!",
  "createdAt": "2024-12-31T18:10:00+05:30"
}
```

---

### 5. WebSocket

#### Connection: `ws://<base>/ws/activities/:id?token=<jwt>`

**Message Types:**

1. **message** - New message received
```json
{
  "type": "message",
  "payload": {
    "id": 123,
    "activityId": 1,
    "userId": 1,
    "userName": "John",
    "text": "Hello!",
    "createdAt": "2024-12-31T18:05:00+05:30"
  }
}
```

2. **join** - User joined activity
```json
{
  "type": "join",
  "payload": {
    "userId": 2,
    "userName": "Jane"
  }
}
```

3. **leave** - User left activity
```json
{
  "type": "leave",
  "payload": {
    "userId": 2
  }
}
```

4. **heading_now** - User confirmed (heading now)
```json
{
  "type": "heading_now",
  "payload": {
    "userId": 2
  }
}
```

5. **reveal** - Identities revealed
```json
{
  "type": "reveal",
  "payload": {
    "activityId": 1,
    "participants": [...]
  }
}
```

---

### 6. Reports

#### POST /reports
**Request:**
```json
{
  "targetUserId": 123,
  "activityId": 456,
  "reason": "Inappropriate behavior"
}
```

**Response (200):**
```json
{
  "message": "Report created successfully",
  "reportId": 789
}
```

---

### 7. Contacts

#### POST /contacts/upload
**Request:**
```json
{
  "hashes": [
    "abc123def456...",
    "ghi789jkl012..."
  ]
}
```

**Response (200):**
```json
{
  "mutualsCount": 5
}
```

---

## CORS Configuration

Backend must allow these origins:
- `http://localhost:19006` (Expo dev server)
- `http://localhost:8081` (Expo web)
- `http://localhost:3000` (if using web)
- Your production domain

Headers:
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

---

## Error Response Format

All errors should follow this format:

```json
{
  "error": "Error message here"
}
```

HTTP Status Codes:
- `200` - Success
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden (invite token issues)
- `409` - Conflict (max members reached)
- `429` - Too Many Requests (rate limiting)
- `500` - Internal Server Error

---

## Testing Checklist

Before connecting frontend to backend:

- [ ] All endpoints return expected JSON schemas
- [ ] CORS configured for Expo origins
- [ ] JWT validation works correctly
- [ ] Rate limiting returns 429 with proper message
- [ ] Invite token validation returns 403 for invalid tokens
- [ ] Max members enforcement returns 409 when full
- [ ] WebSocket accepts JWT in query param
- [ ] WebSocket sends correct message types
- [ ] Contact upload accepts array of hashes
- [ ] Activity responses include all new fields
- [ ] Participants array shows anonymized data when `revealIdentities=false`
- [ ] Participants array shows names when `revealIdentities=true`

---

## Manual Testing Steps

1. **Test OTP Flow:**
   - Request OTP → Should get 200
   - Request OTP 4 times quickly → Should get 429 on 4th
   - Verify OTP → Should get token + user

2. **Test Activity Join:**
   - Join normal activity → Should succeed
   - Join invite-only without token → Should get 403
   - Join invite-only with token → Should succeed
   - Join full activity → Should get 409

3. **Test WebSocket:**
   - Connect to WS endpoint → Should connect
   - Send message via REST → Should appear in WS
   - Disconnect WS → Should fallback to polling

4. **Test Contacts:**
   - Upload hashes → Should return mutualsCount
   - Check mutualsCount appears in activities

5. **Test Identity Reveal:**
   - Create activity with <3 participants → revealIdentities=false
   - Add 3+ participants → revealIdentities=true
   - Check participants show names when revealed

