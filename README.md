# coworking-booking

Spring Boot 3.5 backend for coworking room booking with JWT auth, Flyway migrations, PostgreSQL, and Docker Compose support.

## Local Development

The existing local workflow still works without Docker.

### Run locally

```bash
mvn spring-boot:run
```

By default the local app still expects:

- App: `http://localhost:8082`
- PostgreSQL: `localhost:5436`
- Swagger UI: `http://localhost:8082/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`
- OpenAPI YAML: `http://localhost:8082/v3/api-docs.yaml`

You can also override config with environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_EXPIRATION`
- `JWT_REFRESH_TOKEN_EXPIRATION`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

## Docker

Docker Compose runs:

- App on host port `8082` by default
- PostgreSQL on host port `15436` by default
- Swagger UI on `http://localhost:8082/swagger-ui/index.html`

Because Docker now also uses host port `8082`, stop any locally running Spring Boot process before starting the Compose app, or override `APP_HOST_PORT` in `.env`.

### 1. Prepare environment

```bash
cp .env.example .env
```

Update `.env` with real values, especially:

- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

### 2. Build and start

```bash
docker compose up -d --build
```

### 3. View logs

```bash
docker compose logs -f app
docker compose logs -f db
```

### 4. Stop containers

```bash
docker compose down
```

### 5. Stop containers and remove database volume

```bash
docker compose down -v
```

### 6. Rebuild after code changes

```bash
docker compose up -d --build
```

### 7. Inspect health

```bash
docker compose ps
curl http://localhost:8082/actuator/health
```

## Docker Services

### `db`

- Image: `postgres:17-alpine`
- Persistent named volume: `coworking-booking-postgres-data`
- Healthcheck: `pg_isready`

### `app`

- Multi-stage Docker build
- Runs with Spring profile `docker`
- Waits for PostgreSQL health before starting
- Healthcheck uses Actuator health endpoint
- Flyway runs automatically on startup

## Useful Commands

### Build the jar locally

```bash
mvn -q -DskipTests package
```

### Run unit and slice tests

```bash
mvn -q -Dtest=AuthServiceTest,BookingServiceTest,BookingMaintenanceServiceTest,RoomServiceTest,UserServiceTest,AuthControllerTest,BookingControllerTest,RoomControllerTest,UserControllerTest test
```

### Run integration tests

```bash
mvn -q -Dtest=CoworkingBookingApplicationTests,AuthFlowIntegrationTest,BookingFlowIntegrationTest,BookingMaintenanceIntegrationTest,RoomFlowIntegrationTest test
```

### Run all tests

```bash
mvn -q test
```

## Manual Docker Verification

1. Start the stack with `docker compose up -d --build`.
2. Confirm both services are healthy with `docker compose ps`.
3. Check app health at `http://localhost:8082/actuator/health`.
4. Verify Flyway ran by checking app logs for migration success.
5. Register/login through the API on `http://localhost:8082`.
6. Confirm the app can create and query data against the Docker PostgreSQL instance.
7. Open Swagger UI at `http://localhost:8082/swagger-ui/index.html` and verify protected endpoints work after authorizing with a JWT access token.
