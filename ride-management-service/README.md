# Ride Management Service

Ride Management owns ride requests, driver assignment, ride lifecycle state, status history, final fare calculation, and simulated ride receipts for RideLink. It is a Java 21 / Spring Boot service with its own PostgreSQL database. Passenger and driver IDs are opaque references to Account and Driver services; this service does not query or write their databases.

## Architecture

```mermaid
flowchart LR
  Client[Passenger / Driver / Admin] -->|JWT + REST| Ride[Ride Management :3003]
  Account[Account Service] -. JWT issuer and identity validation .-> Ride
  Ride -->|GET available drivers| Driver[Driver & Vehicle Service]
  Ride --> RideDB[(Ride PostgreSQL)]
  Ride -. transactional outbox .-> MQ[RabbitMQ]
  MQ -. ride.completed .-> Payment[Fare & Payment Service]
```

## Prerequisites and startup

- Java 21.
- PostgreSQL 14+ and a dedicated `ridelink_ride_db` database/user.
- Account Service running at `ACCOUNT_SERVICE_URL` and Driver & Vehicle Service running at `DRIVER_SERVICE_URL` for identity checks and driver assignment.
- Account, Driver & Vehicle, Ride Management, and Fare & Payment must use the same HMAC JWT secret. Use a private value with at least 32 bytes and configure it as `JWT_SECRET`.
- RabbitMQ is required for ride completion events; Ride Management writes to a transactional outbox and Fare & Payment consumes from a durable queue.
- The included Gradle wrapper downloads the pinned Gradle version on first use if needed.

Start the local PostgreSQL database with Docker, then start Account Service and Driver & Vehicle Service if you need their identity and driver assignment features. The database credentials below match the defaults in `application.properties`; change them before using this setup beyond local development. Configure the environment from `.env.example` (Spring Boot does not load `.env` automatically), then start Ride Management:

```powershell
docker compose up -d ride-postgres
npm.cmd run dev
```

Stop the database with `docker compose down`. The database volume is preserved; use `docker compose down -v` only when you intentionally want to delete local database data.

The service listens on port `3003` by default. Flyway applies migrations on startup. Health: `http://localhost:3003/health`; Swagger UI: `http://localhost:3003/api-docs`; the complete static OpenAPI contract is served at `http://localhost:3003/openapi.yaml` and the generated Springdoc document at `http://localhost:3003/v3/api-docs`.

## Commands

Run from the `ride-management-service` directory:

| Task | Command |
| --- | --- |
| Resolve dependencies | `.\gradlew dependencies` (PowerShell) |
| Run locally | `npm.cmd run dev` (PowerShell) or `npm run dev` (other shells) |
| Lint | `.\gradlew checkstyleMain checkstyleTest` (PowerShell) |
| Unit tests | `.\gradlew test` (PowerShell) |
| Coverage report | `.\gradlew jacocoTestReport` (PowerShell) |
| Build | `.\gradlew clean build` (PowerShell) |
| Swagger UI | Open `http://localhost:3003/api-docs` |

## Configuration

See `.env.example`. Important variables are `RIDE_DB_URL`, `RIDE_DB_USERNAME`, `RIDE_DB_PASSWORD`, `JWT_SECRET`, `ACCOUNT_SERVICE_URL`, `DRIVER_SERVICE_URL`, `FARE_PAYMENT_SERVICE_URL`, `DRIVER_SEARCH_RADIUS_KM`, and `RABBITMQ_*`. Never commit a real `.env` or secrets.

## API endpoints

All `/api/rides` routes require `Authorization: Bearer <JWT>`.

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/api/rides` | PASSENGER creates a request |
| `GET` | `/api/rides` | User sees own rides; ADMIN may filter by passengerId, driverId, status |
| `GET` | `/api/rides/{id}` | Passenger/assigned driver owner or ADMIN |
| `POST` | `/api/rides/{id}/assign` | ADMIN/SYSTEM; body `{ "driverId": "<uuid>" }` or omit body to select nearest |
| `POST` | `/api/rides/{id}/accept` | Assigned DRIVER |
| `POST` | `/api/rides/{id}/start` | Assigned DRIVER |
| `POST` | `/api/rides/{id}/complete` | Assigned DRIVER |
| `POST` | `/api/rides/{id}/cancel` | Owning PASSENGER, assigned DRIVER, or ADMIN; body `{ "reason": "..." }` |
| `GET` | `/api/rides/fare-estimate?pickupLat=&pickupLng=&destLat=&destLng=&durationMin=` | Authenticated user |
| `GET` | `/api/rides/{id}/receipt` | Ride owner or ADMIN after completion |

Errors use `{ "error": { "code": "...", "message": "...", "details": [] } }`. Invalid state changes return HTTP 400; no driver returns 409; a simulated payment failure returns 402.

## Ride lifecycle

| Current | Allowed next status |
| --- | --- |
| REQUESTED | ASSIGNED, CANCELLED |
| ASSIGNED | ACCEPTED, CANCELLED |
| ACCEPTED | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | COMPLETED |
| COMPLETED | none |
| CANCELLED | none |

Ride rows are locked while state changes are validated and committed; each change creates a `ride_status_history` record in the same transaction.

## Fare calculation

Pickup and destination distance is calculated with the Haversine formula using Earth radius 6,371.0088 km. The documented rule is `fare = (baseFare + distanceKm * perKmRate + durationMin * perMinRate) * surgeMultiplier`, rounded to two decimal places, with a minimum fare of LKR 300. `baseFare` is LKR 200, `perKmRate` is LKR 50, and `perMinRate` is LKR 5. The peak multiplier is 1.5 from 07:00–09:00 and 17:00–19:00 Asia/Colombo time; otherwise it is 1.0. Completion recalculates the final fare using the stored trip distance and duration.

## Driver assignment

The service calls Driver & Vehicle Service synchronously with `GET /api/drivers/available?lat=&lng=&radius=` because assignment needs an immediate availability response. It forwards the admin bearer token. It filters the response against the ride's optional `serviceArea` (case-insensitive exact match) when `DRIVER_SERVICE_AREA_CHECK=true`, sorts by Haversine distance ascending, selects the nearest driver, and transitions REQUESTED to ASSIGNED. Include `serviceArea` in ride creation when this check is enabled; a missing area then produces HTTP 409 `NO_AVAILABLE_DRIVER`. Account identity/role validation is synchronous because it gates ride creation and driver assignment/acceptance.

On completion, Ride Management writes a versioned `ride.completed` event to its PostgreSQL outbox in the same transaction as the COMPLETED state. A scheduled publisher retries it until RabbitMQ confirms delivery. Fare & Payment owns payment records and consumes the event to create the simulated payment. Ride Management no longer stores a duplicate payment record; its receipt endpoint reads payment details from Fare & Payment and can briefly return `PAYMENT_PROCESSING` while the event consumer runs. Repeated consumer failures are sent to Fare & Payment's dead-letter queue for operator investigation.

## Local demo data

There are no hard-coded credentials. Obtain passenger, driver, and admin JWTs from Account Service and set the Postman collection variables `passengerToken`, `driverToken`, and `adminToken`. Example coordinates in the collection use Colombo Fort (`6.9271, 79.8612`) and Rajagiriya (`6.9147, 79.9729`). Import `postman/ride-management.postman_collection.json` into Postman.

## CI and Git workflow

`.github/workflows/ci.yml` checks Java 21, Checkstyle, tests, coverage, and build on pushes and pull requests to `main` and `develop`. Suggested branch: `feature/ride-management-service`; use conventional commit prefixes such as `feat:`, `fix:`, `test:`, and `docs:` before opening a PR to `develop`.
