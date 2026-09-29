# RideLink Fare & Payment Service

[![Fare & Payment Service CI](https://github.com/it24101192/RideLink/actions/workflows/fare-payment-ci.yml/badge.svg?branch=main)](https://github.com/it24101192/RideLink/actions/workflows/fare-payment-ci.yml)

## Service Overview

Fare & Payment is the RideLink service responsible for fare estimates, final fare calculations, simulated payment outcomes, and payment receipts. It offers versioned REST/JSON APIs and publishes/consumes payment and ride lifecycle events.

Its boundary includes its own fare-estimate and payment records, fare calculation, simulated transaction status, and receipt representation. It owns no passenger, driver, vehicle, or ride lifecycle records.

Out of scope:

- Account registration, passwords, and JWT issuance (Account Service owns these).
- Driver/vehicle registration, availability, and matching (Driver & Vehicle Service owns these).
- Ride creation, assignment, state transitions, and authoritative trip data (Ride Management owns these).
- Real payment processing, card details, payment gateway integration, and financial settlement. This service only simulates outcomes.
- Cross-service database reads/writes. It communicates through APIs and events only.

## Architecture

Fare & Payment sits downstream of Account for JWT validation and Ride Management for completed-ride verification and completion events. It can return payment/fare events for other services to consume. The file event log is a lightweight local/demo adapter for the messaging interface.

**Data ownership:** its MySQL schema, created by Flyway, stores only FareEstimate and Payment records. Ride, passenger, and driver IDs are stable references/opaque identifiers, not foreign keys into another service. No other service accesses this database directly.

```mermaid
flowchart LR
    Passenger[Passenger / Driver / Admin] -->|Bearer JWT, REST| Fare[Fare & Payment Service]
    Account[Account Service: JWT issuer] -. signs shared JWT .-> Fare
    Fare -->|GET completed ride| Ride[Ride Management Service]
    Ride -. ride.completed event .-> Log[(JSONL event log)]
    Fare <-->|subscribe / publish| Log
    Fare -->|fare estimates, payments| DB[(Fare & Payment MySQL)]
    Fare -. payment.completed / fare.calculated .-> Other[RideLink consumers]
```

## Prerequisites

- Java 21 (Temurin or another compatible JDK).
- MySQL 8.x with a database/schema created for this service (default: `ridelink_fare_db`).
- Gradle wrapper is shared from the repository root at `account-service/gradlew`; Gradle downloads the project dependencies on first run.
- No external message broker is required for the current demo: messaging uses a local JSON Lines file. A broker can replace this adapter later.
- Account Service for issuing compatible JWTs; Ride Management Service must be reachable to process a payment. Fare estimation itself only requires this service and its database.

## Configuration

From the repository root, make a local environment file:

```bash
cp fare-payment-service/.env.example fare-payment-service/.env
```

Edit the values for your machine. Spring Boot does **not** automatically load `.env`; export it before starting the service:

```bash
set -a
source fare-payment-service/.env
set +a
```

| Variable | Required | Description | Example |
| --- | --- | --- | --- |
| `SERVER_PORT` | No | HTTP listen port (default `3004`). | `3004` |
| `FARE_DB_URL` | No | JDBC URL for this service's MySQL database. | `jdbc:mysql://localhost:3306/ridelink_fare_db` |
| `FARE_DB_USERNAME` | No | MySQL user (default `root`). | `root` |
| `FARE_DB_PASSWORD` | Yes for local DB | MySQL password. | `change-me` |
| `FARE_JWT_SECRET` | Yes | Shared HMAC secret used to validate Account Service JWTs; use a local-only value of at least 32 bytes and keep it out of Git. If unset, `RIDELINK_JWT_SECRET` is used. | `local-only-please-replace-32-byte-key` |
| `RIDELINK_JWT_SECRET` | Fallback | Shared JWT secret fallback for compatibility with Account Service configuration. | `local-only-please-replace-32-byte-key` |
| `RIDE_SERVICE_URL` | No | Ride Management base URL for synchronous ride lookup (default `http://localhost:3003`). | `http://localhost:3003` |
| `MESSAGE_BROKER_TYPE` | No | Current adapter selector; supported demo value is `file`. | `file` |
| `MESSAGE_BROKER_URL` | No | `file:` URI for the JSONL event log. | `file:/tmp/ridelink-events.jsonl` |
| `PAYMENT_TEST_MODE` | No | Enables deterministic payment result override; keep off for normal demo behavior. | `false` |
| `PAYMENT_TEST_OUTCOME_SUCCESS` | No | Result when test mode is enabled. | `true` |

Do not commit `.env` or production secrets. `.env.example` contains safe placeholders, not usable credentials.

## Start-up Order

From the RideLink repository root:

1. Start MySQL and create the configured database, for example `ridelink_fare_db`.
2. Start Account Service so it can issue JWTs using the shared HMAC secret.
3. Start Ride Management Service at the configured `RIDE_SERVICE_URL` before testing payment creation. Its ride lookup must return `COMPLETED` for the ride being paid.
4. Configure and start Fare & Payment. Fare & Payment applies its Flyway migrations automatically on startup.

```bash
cp fare-payment-service/.env.example fare-payment-service/.env
# Edit .env, then export it in this shell:
set -a && source fare-payment-service/.env && set +a
bash account-service/gradlew -p fare-payment-service bootRun
```

Health check: <http://localhost:3004/health>. Swagger UI: <http://localhost:3004/api-docs>.

## Commands

Run commands from the repository root. The wrapper handles Gradle installation and dependencies; there is no separate `npm install` step.

| Task | Command |
| --- | --- |
| Install/resolve dependencies | `bash account-service/gradlew -p fare-payment-service dependencies` |
| Development server | `bash account-service/gradlew -p fare-payment-service bootRun` |
| Build/package | `bash account-service/gradlew -p fare-payment-service build` |
| Unit tests | `bash account-service/gradlew -p fare-payment-service test` |
| Integration/API tests | `bash account-service/gradlew -p fare-payment-service integrationTest` |
| All verification | `bash account-service/gradlew -p fare-payment-service check` |
| Lint | `bash account-service/gradlew -p fare-payment-service checkstyleMain checkstyleTest` |
| Coverage report | `bash account-service/gradlew -p fare-payment-service jacocoTestReport` |
| Migrate | `bootRun` runs Flyway migrations automatically; migration SQL is in `src/main/resources/db/migration/`. |
| Seed | No seed task or seeded records are included. Use Ride Management sample rides and API/Postman requests. |

## API Endpoints

All API paths are under `/api/v1`. Fare/payment routes require a Bearer JWT with `userId` and `role` claims. Passengers and drivers may access their own records; admins may access records across users. Refunds are admin-only. Health and Swagger docs are public.

| Method | Path | Auth role | Description |
| --- | --- | --- | --- |
| `GET` | `/health` | Public | Liveness check. |
| `POST` | `/api/v1/fares/estimate` | Passenger, driver, admin | Calculate and store a fare estimate. |
| `GET` | `/api/v1/fares/{id}` | Passenger, driver, admin (owner or admin) | Retrieve a fare estimate. |
| `POST` | `/api/v1/payments` | Passenger, driver, admin | Verify completed ride, calculate final fare, and record simulated outcome. |
| `GET` | `/api/v1/payments/{id}` | Passenger, driver, admin (ride participant or admin) | Retrieve a payment. |
| `GET` | `/api/v1/payments/ride/{rideId}` | Passenger, driver, admin (ride participant or admin) | Retrieve payment records for a ride. |
| `GET` | `/api/v1/payments/{id}/receipt` | Passenger, driver, admin (ride participant or admin) | Retrieve a receipt derived from a payment. |
| `POST` | `/api/v1/payments/{id}/refund` | Admin | Mark an eligible payment as refunded. |

Interactive API docs: [`/api-docs`](http://localhost:3004/api-docs); OpenAPI JSON: `/v3/api-docs`.

## Sample Credentials & Test Data

Never use a real credential or secret in examples. For local use, set a private throwaway `FARE_JWT_SECRET` of 32 bytes or more and configure Account Service with the same secret. Obtain a token from the locally running Account Service, or create a short-lived local-only HS256 token with these claims (the fare service requires `userId` and `role`; `sub` is also included). For this HS256 example, use a 32–47 byte secret; the service chooses HS384/HS512 for longer keys:

```python
# Local development only. Requires: pip install PyJWT
import os, jwt
from datetime import datetime, timedelta, timezone

secret = os.environ["FARE_JWT_SECRET"]
token = jwt.encode({
    "sub": "local-passenger-1001",
    "userId": "1001",
    "role": "PASSENGER",
    "iat": datetime.now(timezone.utc),
    "exp": datetime.now(timezone.utc) + timedelta(hours=1),
}, secret, algorithm="HS256")
print(token)  # paste into Postman; never commit or use outside local development
```

The example Postman environment uses placeholder IDs and a placeholder `token`; replace these with IDs for records in your running Ride Management and Fare & Payment services. Sample IDs are not pre-seeded and may not exist.

Create a fare estimate:

```bash
curl -X POST http://localhost:3004/api/v1/fares/estimate \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"pickupLocation":"Colombo Fort","destinationLocation":"Bambalapitiya","distanceKm":5.2,"durationMinutes":18,"surgeMultiplier":1.0}'
```

Process a payment (use a real completed ride UUID, and passenger/driver IDs consistent with Ride Management and the JWT):

```bash
curl -X POST http://localhost:3004/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"rideId":"dddddddd-dddd-4ddd-8ddd-dddddddddddd","passengerId":"1001","driverId":"2001","actualDistanceKm":5.2,"actualDurationMinutes":18,"surgeMultiplier":1.0,"paymentMethod":"SIMULATED_CARD","transactionRef":"local-txn-123"}'
```

Fare rule: **LKR 200 base + LKR 80/km + LKR 10/minute**, multiplied by optional surge (`1.0` default, range `1.0`–`2.5`), then a minimum fare of **LKR 300** is applied. Amounts are represented and stored as integer cents (100 cents = LKR 1); component amounts are rounded half-up.

## Interservice Communication

### Synchronous REST

Before recording payment, the service calls `GET {RIDE_SERVICE_URL}/api/v1/rides/{rideId}`. A payment proceeds only after Ride Management confirms that the ride exists and is `COMPLETED`. The typed client has timeouts and retries transient failures twice; a missing ride maps to `404`, a non-completed ride to `409`, and Ride Management unavailability to `503`. This is synchronous because payment must be gated on the authoritative current ride state. A queue-only validation was considered and rejected for this decision because it could act on delayed or missing state updates.

### Asynchronous events

The `MessagingClient` abstraction publishes `payment.completed` after payment processing and subscribes to `ride.completed`. The ride event triggers final fare calculation and publication of `fare.calculated`. Events are appended/read as JSON Lines in the configured local file (`MESSAGE_BROKER_URL`), which keeps this assignment easy to run without broker infrastructure and demonstrates asynchronous decoupling. An example `ride.completed` envelope is:

```json
{"topic":"ride.completed","timestamp":"2026-09-27T00:00:00Z","payload":{"rideId":"dddddddd-dddd-4ddd-8ddd-dddddddddddd","actualDistanceKm":5.2,"actualDurationMinutes":18,"surgeMultiplier":1.0}}
```

RabbitMQ was considered and rejected for the current demo because it adds broker deployment/configuration overhead. The `MessagingClient` port allows a durable broker adapter later. The file adapter is for a single-process demonstration and does not provide distributed coordination, durable delivery guarantees, or production replay semantics.

## Testing

Unit tests (domain, application services, and client adapters):

```bash
bash account-service/gradlew -p fare-payment-service test
```

API integration tests use Spring MVC test infrastructure and mocked service boundaries; they do not require live Ride Management or Account Service instances:

```bash
bash account-service/gradlew -p fare-payment-service integrationTest
```

The combined `check` task runs Checkstyle and both test tasks. Generate HTML/XML coverage reports with `jacocoTestReport`; in CI, reports are uploaded as workflow artifacts.

For manual API scenarios, import [`postman/RideLink-Fare-Payment.postman_collection.json`](postman/RideLink-Fare-Payment.postman_collection.json) and [`postman/RideLink-local.postman_environment.json`](postman/RideLink-local.postman_environment.json) into Postman. Select the environment, set `token` to a local Account Service JWT, then replace ride/record IDs with valid values.

## Negative Scenarios

| Scenario | Expected response |
| --- | --- |
| Missing/invalid JWT | `401 Unauthorized` |
| Role not allowed for API | `403 Forbidden` |
| User reads another participant's estimate/payment/receipt | `403 Forbidden` |
| Invalid request DTO (missing required values or malformed body) | `400 Bad Request` |
| Domain-invalid distance, duration, or surge | `422 Unprocessable Entity` |
| Unknown ride or record ID | `404 Not Found` |
| Ride is not `COMPLETED` | `409 Conflict` |
| Duplicate payment for ride / conflicting transaction | `409 Conflict` |
| Simulated card decline (`transactionRef` ends in `0000`) | `402 Payment Required`, body includes `FAILED` status and `failureReason` |
| Ride Management unavailable after retries | `503 Service Unavailable` |

Error responses use `{ "error": { "code": "...", "message": "...", "details": [...] } }` except a simulated payment decline, which returns the persisted failed payment DTO so callers can retain its payment ID and failure reason.

## Limitations & Future Improvements

- Payment is simulated; no card data, gateway, settlement, or actual refund transfer is involved.
- The file event adapter is single-process and intended only for demonstration. Replace it with RabbitMQ/Kafka or another durable broker for production delivery, retries, and consumer groups.
- Ride Management and Account Service availability affect payments and authenticated access respectively; add distributed tracing, metrics, and operational dashboards for deployment environments.
- Fare rates and currency are currently fixed to the documented LKR rule; introduce versioned pricing/configuration and regional currencies if requirements expand.
- Idempotency and concurrent duplicate-payment protection should be backed by explicit database constraints/transaction handling for production workloads.
- Add seed tooling and end-to-end tests against containerized dependencies if the assignment environment requires reproducible full-system scenarios.
