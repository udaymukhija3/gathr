# gathr Backend

Backend API for gathr - small group hangouts around activities in Gurgaon.

## Tech Stack

- **Backend**: Spring Boot 3.2.0
- **Database**: PostgreSQL
- **ORM**: Hibernate/JPA
- **Authentication**: JWT with mock OTP
- **Containerization**: Docker

## Features

- User authentication with phone OTP (mock implementation)
- Hub management
- Activity creation and management
- Activity participation (interested/confirmed)
- Real-time messaging for activities
- RESTful API with JSON responses

## Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 15+ (or use Docker Compose)
- Docker (optional, for containerized deployment)

## Environment Variables

Create a `.env` file or set the following environment variables:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gathr
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=your-secret-key-min-256-bits
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

## Running with Docker Compose

1. Build and start all services:
```bash
docker-compose up --build
```

2. The API will be available at `http://localhost:8080`

## Running Locally

1. Start PostgreSQL database:
```bash
docker run -d --name gathr-postgres \
  -e POSTGRES_DB=gathr \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

2. Build the project:
```bash
mvn clean package
```

3. Run the application:
```bash
java -jar target/gathr-backend-1.0.0.jar
```

Or use Maven:
```bash
mvn spring-boot:run
```

## API Endpoints

### Authentication

- `POST /auth/otp/start` - Start OTP verification (mock)
  - Body: `{ "phone": "1234567890" }`
  - Rate limited: Max 3 requests per hour per phone
  - Returns: `{ "message": "OTP sent successfully" }` or `429 Too Many Requests`
  
- `POST /auth/otp/verify` - Verify OTP and get JWT token
  - Body: `{ "phone": "1234567890", "otp": "123456" }`
  - Returns: `{ "token": "...", "user": {...} }`

### Hubs

- `GET /hubs` - Get all hubs
  - Headers: `Authorization: Bearer <token>`

### Activities

- `GET /activities?hub_id=<id>` - Get today's activities for a hub
  - Headers: `Authorization: Bearer <token>`
  - Returns activities with: `peopleCount`, `mutualsCount`, `isInviteOnly`, `revealIdentities`, `maxMembers`
  
- `GET /activities/:id` - Get activity details
  - Headers: `Authorization: Bearer <token>`
  - Returns activity with participants (anonymized if `revealIdentities=false`)
  
- `POST /activities` - Create a new activity
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "title": "...", "hubId": 1, "category": "SPORTS", "startTime": "...", "endTime": "...", "isInviteOnly": false, "maxMembers": 4 }`
  
- `POST /activities/:id/join?status=INTERESTED&inviteToken=<token>` - Join an activity
  - Headers: `Authorization: Bearer <token>`
  - Query params: 
    - `status` (INTERESTED or CONFIRMED)
    - `inviteToken` (required if activity is invite-only)
  - Returns: `409 Conflict` if max members reached
  - Returns: `403 Forbidden` if invite token missing/invalid for invite-only activities
  
- `POST /activities/:id/invite-token` - Generate invite token for invite-only activity
  - Headers: `Authorization: Bearer <token>`
  - Returns: `{ "token": "...", "expiresAt": "..." }`

### Messages

- `GET /activities/:id/messages` - Get messages for an activity
  - Headers: `Authorization: Bearer <token>`
  - Messages are automatically deleted 24 hours after activity end time
  
- `POST /activities/:id/messages` - Send a message
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "text": "..." }`
  - Messages are ephemeral and auto-deleted after activity ends

### Reports & Moderation

- `POST /reports` - Create a report
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "targetUserId": 123, "activityId": 456, "reason": "Inappropriate behavior" }`
  - Automatically sends notification to Slack webhook (if configured)
  - Logs event: `report_created`

### Contacts & Mutuals

- `POST /contacts/upload` - Upload hashed phone contacts
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "hashes": ["hash1", "hash2", ...] }`
  - Returns: `{ "mutualsCount": 5 }`
  - Used to calculate mutual contacts count for activities

## Seed Data

The application automatically seeds the database with:
- 3 hubs: Cyberhub, Galleria, 32nd Avenue
- 5 sample activities across different categories
- 1 test user (phone: 1234567890)

## Mock OTP

For testing, the mock OTP is always `123456`. In production, integrate with a real SMS service.

**Rate Limiting**: OTP requests are limited to 3 per hour per phone number. Exceeding this limit returns `429 Too Many Requests`.

## Features

### Reports & Moderation
- User reporting system with Slack webhook notifications
- Report status tracking (OPEN, CLOSED, etc.)
- Event logging for all reports

### Invite-Only Activities
- Generate invite tokens for activities
- Enforce invite token validation on join
- Token expiry (default: 48 hours)

### Event Logging
- Universal event logging system
- Tracks: activity_created, activity_joined, message_sent, report_created, invite_token_generated
- JSONB properties for flexible event data

### Mutual Contacts
- Upload hashed phone contacts
- Calculate mutual contacts count
- Display mutuals count in activity participants

### Identity Reveal
- Participants anonymized until threshold reached
- Reveals identities when >=3 confirmed OR >=3 interested
- Configurable per activity

### Max Group Size
- Default max members: 4
- Configurable per activity
- Enforced on join (returns 409 if full)

### Message Expiry
- Messages auto-deleted 24 hours after activity end time
- Scheduled job runs every 15 minutes
- Ephemeral chat for privacy

## Slack Webhook Setup

1. Create a Slack webhook URL in your Slack workspace
2. Set `SLACK_WEBHOOK_URL` environment variable
3. Reports will automatically send notifications to Slack

Example curl for creating a report:
```bash
curl -X POST http://localhost:8080/reports \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUserId": 123,
    "activityId": 456,
    "reason": "Inappropriate behavior"
  }'
```

## Invite Token Flow

1. Create an invite-only activity:
```bash
curl -X POST http://localhost:8080/activities \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Private Event",
    "hubId": 1,
    "category": "FOOD",
    "startTime": "2024-01-01T18:00:00",
    "endTime": "2024-01-01T20:00:00",
    "isInviteOnly": true
  }'
```

2. Generate invite token:
```bash
curl -X POST http://localhost:8080/activities/1/invite-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

3. Join with token:
```bash
curl -X POST "http://localhost:8080/activities/1/join?status=INTERESTED&inviteToken=TOKEN_HERE" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Project Structure

```
src/
├── main/
│   ├── java/com/gathr/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data transfer objects
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Exception handlers
│   │   ├── repository/      # JPA repositories
│   │   ├── security/        # JWT and security
│   │   └── service/         # Business logic
│   └── resources/
│       └── application.properties
└── test/
```

## License

MIT

