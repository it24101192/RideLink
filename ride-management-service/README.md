# Ride Management Service

Ride Management owns ride requests, driver assignment, ride lifecycle state, status history, final fare calculation, and simulated ride receipts for RideLink. It is a Java 21 / Spring Boot service with its own PostgreSQL database. Passenger and driver IDs are opaque references to Account and Driver services; this service does not query or write their databases.

## Architecture

```mermaid
flowchart LR
  Client[Passenger / Driver / Admin] -->|JWT + REST| Ride[Ride Management :3003]
  Account[Account Service] -. JWT issuer and identity validation .-> Ride
  Ride -->|GET available drivers| Driver[Driver & Vehicle Service]
  Ride --> RideDB[(Ride PostgreSQL)]
  Ride -. ride.completed JSONL event .-> Events[(Shared local event log)]
  Events -. consumed by .-> Payment[Fare & Payment Service]
```

## Prerequisites and startup

- Java 21.
- PostgreSQL 14+ and a dedicated `ridelink_ride_db` database/user.
- Account Service running at `ACCOUNT_SERVICE_URL` and Driver & Vehicle Service running at `DRIVER_SERVICE_URL` for identity checks and driver assignment.
- The Account Service and Ride Management Service must use the same HMAC JWT secret. Use a locally generated secret with at least 32 bytes.
- Gradle wrapper at the repository root: `account-service/gradlew`.

Start PostgreSQL, Account Service, and Driver & Vehicle Service first. Configure the environment from `.env.example` (Spring Boot does not load `.env` automatically), then start Ride Management:

```bash
cp ride-management-service/.env.example ride-management-service/.env
# Export the variables in your shell after editing the local file.
bash account-service/gradlew -p ride-management-service bootRun
```

The service listens on port `3003` by default. Flyway applies migrations on startup. Health: `http://localhost:3003/health`; Swagger UI: `http://localhost:3003/api-docs`; the complete static OpenAPI contract is served at `http://localhost:3003/openapi.yaml` and the generated Springdoc document at `http://localhost:3003/v3/api-docs`.

## Commands

Run from repository root:

| Task | Command |
| --- | --- |
| Resolve dependencies | `bash account-service/gradlew -p ride-management-service dependencies` |
| Run locally | `bash account-service/gradlew -p ride-management-service bootRun` |
| Lint | `bash account-service/gradlew -p ride-management-service checkstyleMain checkstyleTest` |
| Unit tests | `bash account-service/gradlew -p ride-management-service test` |
| Coverage report | `bash account-service/gradlew -p ride-management-service jacocoTestReport` |
| Build | `bash account-service/gradlew -p ride-management-service clean build` |
| Swagger UI | Open `http://localhost:3003/api-docs` |

## Configuration

See `.env.example`. Important variables are `RIDE_DB_URL`, `RIDE_DB_USERNAME`, `RIDE_DB_PASSWORD`, `RIDE_JWT_SECRET`, `ACCOUNT_SERVICE_URL`, `DRIVER_SERVICE_URL`, `DRIVER_SEARCH_RADIUS_KM`, and `MESSAGE_BROKER_URL`. Never commit a real `.env` or secrets.

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

The service calls Driver & Vehicle Service synchronously with `GET /api/drivers/available?lat=&lng=&radius=` because assignment needs an immediate availability response. It filters the response by service area when `DRIVER_SERVICE_AREA_CHECK=true`, sorts by Haversine distance ascending, selects the nearest driver, and transitions REQUESTED to ASSIGNED. An empty result returns HTTP 409 `NO_AVAILABLE_DRIVER`. Account identity/role validation is synchronous because it gates ride creation and driver assignment/acceptance.

On completion, the service writes a `ride.completed` event as JSON Lines to `MESSAGE_BROKER_URL`; the existing Fare & Payment service can consume that shared file adapter. Set `MESSAGE_BROKER_TYPE=file` for the local adapter. A production deployment should replace the file adapter with a durable broker and an outbox so event publication survives process failures.

## Local demo data

There are no hard-coded credentials. Obtain passenger, driver, and admin JWTs from Account Service and set the Postman collection variables `passengerToken`, `driverToken`, and `adminToken`. Example coordinates in the collection use Colombo Fort (`6.9271, 79.8612`) and Rajagiriya (`6.9147, 79.9729`). Import `postman/ride-management.postman_collection.json` into Postman.

## CI and Git workflow

`.github/workflows/ci.yml` checks Java 21, Checkstyle, tests, coverage, and build on pushes and pull requests to `main` and `develop`. Suggested branch: `feature/ride-management-service`; use conventional commit prefixes such as `feat:`, `fix:`, `test:`, and `docs:` before opening a PR to `develop`.
