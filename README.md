# api-identity

The shared identity and workspace service for the M2 suite. Handles user auto-provisioning on first login, workspace membership, and workspace invitations for every app in the suite (api-movements, api-keep). Not exposed to the internet — only consumed internally by the other backends.

## Features

- **User auto-provisioning**: a user is created on first login, per-app, resolved from the JWT's `app` claim (set per-client in Keycloak) rather than a client-supplied header
- **Workspaces & membership**: create/list/delete workspaces, with role-based membership (`OWNER`/`COLLABORATOR`/`READ_ONLY`)
- **Invitations**: invite a user to a workspace by email, accept/decline, rate-limited per user (10 invitation batches/hour) to prevent abuse
- **Onboarding**: track first-login and product-tour completion per user
- **Rate limiting**: Redis-backed, applied to invitation-sending and user creation (20/hour), fails open if Redis is unavailable
- **Cross-app events**: invitation and workspace events published to RabbitMQ, consumed by api-movements and api-keep
- **User authentication**: Keycloak OAuth2 / JWT (RS256) resource server
- **API documentation**: Swagger/OpenAPI UI
- **Database migrations**: Liquibase (`ddl-auto: none`)

## Tech Stack

- **Java 25** with **Spring Boot 4.1**
- **MySQL 8.0** database
- **Liquibase** for database migrations
- **MapStruct** for object mapping
- **Spring Security** with OAuth2 / Keycloak JWT
- **Spring Web** for REST endpoints
- **Spring Data JPA** for data access
- **RabbitMQ** for publishing invitation/workspace events, consumed by api-movements and api-keep
- **Redis** for rate limiting (fixed-window counters on invitations and user creation)
- **GraalVM native image** for the production build (see `Dockerfile`)
- **Spock** for testing

## Prerequisites

- Java 25 JDK (GraalVM distribution, for native builds)
- Docker and Docker Compose
- MySQL 8.0+, RabbitMQ, and Redis (or use the provided Docker Compose setup for MySQL/RabbitMQ)
- Gradle 9+

## Getting Started

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/matiasferrerovilas/api-identity.git
   cd api-identity
   ```

2. **Set up dependencies**
   - Create a MySQL database named `identity`
   - Or use the provided Docker Compose setup (MySQL + RabbitMQ):
     ```bash
     docker compose -f docker-compose/docker-compose.yml up -d
     ```
   - A local Redis instance is also expected (`localhost:6379` by default); rate limiting fails open (never blocks requests) if Redis is unreachable

3. **Configure application properties**
   Create `src/main/resources/application-dev.yaml` (or export env vars) with your database, RabbitMQ, and Redis settings:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/identity
       username: your_username
       password: your_password
     rabbitmq:
       host: localhost
       port: 5672
       username: your_username
       password: your_password
     data:
       redis:
         host: localhost
         port: 6379
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

### Docker Build

Builds a GraalVM native image (see `Dockerfile`):

```bash
docker build -t api-identity .

docker run -p 8081:8080 \
  -e DB_USERNAME=your_username \
  -e DB_PASSWORD=your_password \
  api-identity
```

## API Overview

All endpoints are under `/v1` and require a valid Keycloak-issued JWT unless noted.

| Method | Path | Description |
|---|---|---|
| GET | `/v1/users/me` | Current authenticated user (auto-provisions on first call) |
| GET | `/v1/users` | List users |
| POST | `/v1/users` | Create a user |
| PATCH | `/v1/users/me/type` | Update the current user's type |
| POST | `/v1/workspaces` | Create a workspace |
| GET | `/v1/workspaces/{workspaceId}` | Get a workspace |
| GET | `/v1/workspaces/members` | List members across the caller's workspaces |
| GET | `/v1/workspaces/{workspaceId}/members/{userId}` | Get a specific membership |
| DELETE | `/v1/workspaces/{workspaceId}` | Delete a workspace |
| GET | `/v1/invitations` | List invitations |
| POST | `/v1/invitations/{workspaceId}` | Invite a user to a workspace (rate-limited) |
| PATCH | `/v1/invitations` | Accept/decline an invitation |
| PUT | `/v1/onboarding/tour` | Mark the product tour as completed |
| PATCH | `/v1/onboarding/{userId}/first-login` | Mark first login as completed |

Full interactive documentation is available at `/docs` once the app is running.

## Authentication

Keycloak OAuth2 with JWT (RS256). Include the token in the `Authorization` header as `Bearer <token>`. The calling app is identified from the JWT's `app` claim, and the caller's role from realm/client roles (`ROLE_ADMIN`/`ROLE_FAMILY`/`ROLE_GUEST`, plus `KEYCLOAK_REALM_ADMIN` for `/actuator/**`).

## Rate Limiting

Redis-backed fixed-window counters, applied per user/email:

- Invitations: 10 invitation batches per hour per inviting user
- User creation: 20 per hour per email

If Redis is unavailable, rate limiting fails open (requests are allowed) rather than blocking the service.

## Testing

Run the test suite:

```bash
./gradlew test
```

Run tests and checkstyle together:

```bash
./gradlew test checkstyleMain checkstyleTest
```

## Monitoring

Metrics are available at `/actuator/prometheus` and can be scraped by Prometheus. Health, info, and metrics endpoints are public; the rest of `/actuator/**` requires the `ADMIN` role.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
