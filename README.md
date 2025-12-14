# NKCTF Platform Backend

A modern CTF (Capture The Flag) competition platform backend system built with Java by the NKCTF team.

[中文文档](README.zh-CN.md)

## Features

- **User Management**: Registration, login, profile management with JWT authentication
- **Challenge Management**: Multi-category challenges (Web, PWN, Reverse, Crypto, Misc)
- **Competition System**: Support for individual and team competitions
- **Dynamic Flag**: Per-user unique flags with Docker container isolation
- **Team System**: Team creation, invitation, and captain management
- **Leaderboard**: Real-time scoring and rankings
- **Hint System**: Unlockable hints with point deduction
- **Rate Limiting**: Built-in API rate limiting with Resilience4j
- **API Documentation**: Auto-generated Swagger/OpenAPI docs

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Runtime (Virtual Threads enabled) |
| Spring Boot | 4.0.0 | Web Framework |
| Spring Security | 6.x | Authentication & Authorization |
| MyBatis-Plus | 3.5.15 | ORM Framework |
| PostgreSQL | 16 | Primary Database |
| Redis | 7 | Cache, Session, Distributed Lock |
| Docker Java | 3.4.0 | Container Management |
| JJWT | 0.12.6 | JWT Token Handling |

## Prerequisites

- JDK 21+
- Docker & Docker Compose
- Maven 3.9+ (or use the included `mvnw`)

## Quick Start

### 1. Start Dependencies

```bash
cd docker && docker compose up -d
```

This starts PostgreSQL and Redis containers with default configurations.

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start at `http://localhost:8080`.

### 3. Access API Documentation

Visit: `http://localhost:8080/api/swagger-ui.html`

## Project Structure

```
src/main/java/cn/edu/ndky/nkctf/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── service/         # Service interfaces
│   └── impl/        # Service implementations
├── mapper/          # MyBatis mapper interfaces
├── entity/          # Database entities
├── dto/             # Data transfer objects
│   ├── request/     # Request DTOs
│   └── response/    # Response DTOs
├── security/        # Security components
├── util/            # Utility classes
├── exception/       # Exception definitions
└── NkctfApplication.java

src/main/resources/
├── application.yml  # Main configuration
└── mapper/          # MyBatis XML mappers

docker/
├── docker-compose.yml
└── init-db/         # Database init scripts
```

## Development

### Common Commands

```bash
# Compile
./mvnw compile

# Run tests
./mvnw test

# Package (skip tests)
./mvnw package -DskipTests

# Clean build
./mvnw clean
```

### Database Connection

```
PostgreSQL: localhost:5432/nkctf
  Username: nkctf
  Password: nkctf123456

Redis: localhost:6379
  Password: nkctf123456
```

## API Overview

| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/register` | User registration |
| `POST /api/auth/login` | User login |
| `GET /api/challenges` | List challenges |
| `POST /api/challenges/{id}/submit` | Submit flag |
| `GET /api/competitions` | List competitions |
| `GET /api/leaderboard` | Get leaderboard |
| `GET /api/teams` | Team management |

## Configuration

Key configuration items in `application.yml`:

```yaml
jwt:
  secret: your-secret-key  # Must change in production!
  expiration: 604800000    # 7 days

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nkctf
  redis:
    host: localhost
    port: 6379
```

## Security Notes

1. ==**Production deployment must change**==:
   - JWT secret key
   - Database passwords
   - Redis password

2. **Do not commit sensitive information** to the repository

3. **Container resource limits**: Configure memory and CPU limits to prevent resource exhaustion

4. **Input validation**: All user inputs must be validated to prevent injection attacks

## License

MIT License

## Contributing

Contributions are welcome!
