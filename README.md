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
  
- `POST /auth/otp/verify` - Verify OTP and get JWT token
  - Body: `{ "phone": "1234567890", "otp": "123456" }`
  - Returns: `{ "token": "...", "user": {...} }`

### Hubs

- `GET /hubs` - Get all hubs
  - Headers: `Authorization: Bearer <token>`

### Activities

- `GET /activities?hub_id=<id>` - Get today's activities for a hub
  - Headers: `Authorization: Bearer <token>`
  
- `POST /activities` - Create a new activity
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "title": "...", "hubId": 1, "category": "SPORTS", "startTime": "...", "endTime": "..." }`
  
- `POST /activities/:id/join?status=INTERESTED` - Join an activity
  - Headers: `Authorization: Bearer <token>`
  - Query params: `status` (INTERESTED or CONFIRMED)

### Messages

- `GET /activities/:id/messages` - Get messages for an activity
  - Headers: `Authorization: Bearer <token>`
  
- `POST /activities/:id/messages` - Send a message
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "text": "..." }`

## Seed Data

The application automatically seeds the database with:
- 3 hubs: Cyberhub, Galleria, 32nd Avenue
- 5 sample activities across different categories
- 1 test user (phone: 1234567890)

## Mock OTP

For testing, the mock OTP is always `123456`. In production, integrate with a real SMS service.

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

