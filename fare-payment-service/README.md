# Fare & Payment Service

Fare & Payment owns fare estimates, payment processing, payment records, and receipts. Ride Management owns ride records and lifecycle. The services exchange data through authenticated REST calls and RabbitMQ events; neither reads the other's database.

## Integration

- Validate a payment against `GET ${RIDE_SERVICE_URL}/api/rides/{rideId}`. The service forwards the caller's Bearer JWT, and only processes a ride in `COMPLETED` state.
- Consume `ride.completed` from exchange `ridelink.events`, routing key `ride.completed`, queue `fare-payment.ride-completed`.
- Create one simulated `CASH` payment from the committed event fare. The database enforces one record per `ride_id`; redelivered events return the existing record without creating another.
- Send payment summaries to Ride Management's receipt endpoint through `GET /api/v1/payments/ride/{rideId}`. Ride Management forwards the caller's token.
- Retry event failures three times, then dead-letter to `fare-payment.ride-completed.dlq`.

Ride Management publishes the version `1.0` contract below. Keep the publisher and consumer DTOs aligned:

```json
{
  "eventId": "uuid",
  "eventType": "ride.completed",
  "eventVersion": "1.0",
  "occurredAt": "2026-10-03T12:00:00Z",
  "rideId": "uuid",
  "passengerId": "uuid",
  "driverId": "uuid",
  "distanceKm": 12.5,
  "durationMinutes": 25,
  "fareAmount": 2500.00,
  "currency": "LKR"
}
```

The completion consumer uses the Fare & Payment outcome simulator. Enable `PAYMENT_TEST_MODE=true` and `PAYMENT_TEST_OUTCOME_SUCCESS=true` for deterministic local demonstrations. The regular `POST /api/v1/payments` API remains available for explicitly initiated simulated payments and verifies ride status synchronously; a ride can have only one payment record.

## Local setup

Requirements: Java 21, MySQL 8, and RabbitMQ. Create `ridelink_fare_db`; Flyway applies schema migrations on startup. Configure the same `JWT_SECRET` used by Account, Driver & Vehicle, and Ride Management services. The secret must be at least 32 bytes.

PowerShell, from the repository root:

```powershell
$env:JWT_SECRET = 'replace-with-a-private-secret-of-at-least-32-bytes'
$env:FARE_DB_URL = 'jdbc:mysql://localhost:3306/ridelink_fare_db'
$env:FARE_DB_USERNAME = 'root'
$env:FARE_DB_PASSWORD = 'your-local-mysql-password'
$env:RIDE_SERVICE_URL = 'http://localhost:3003'
$env:RABBITMQ_HOST = 'localhost'
./fare-payment-service/gradlew.bat -p fare-payment-service bootRun
```

Prefer the root Compose stack for all four services and their databases/broker:

```powershell
Copy-Item .env.example .env
# Set JWT_SECRET and private local database/broker passwords in .env.
docker compose up --build -d
```

Fare & Payment listens on port `3004`. Health is `http://localhost:3004/health`; Swagger UI is `http://localhost:3004/api-docs`.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `3004` | HTTP port |
| `JWT_SECRET` | required | Shared HMAC JWT secret, at least 32 bytes |
| `FARE_DB_URL` | `jdbc:mysql://localhost:3306/ridelink_fare_db` | Fare & Payment database |
| `FARE_DB_USERNAME` | `root` | Database username |
| `FARE_DB_PASSWORD` | required | Database password |
| `RIDE_SERVICE_URL` | `http://localhost:3003` | Ride Management base URL |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ hostname |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `PAYMENT_TEST_MODE` | `false` | Enables deterministic simulator outcome |
| `PAYMENT_TEST_OUTCOME_SUCCESS` | `true` | Simulator result when test mode is enabled |

Inside Compose, the service uses `ride-management-service` and `rabbitmq` hostnames. Do not use `localhost` for service-to-service traffic from a container.

## API

All `/api/v1/**` endpoints require a Bearer JWT. Passenger/driver operations are scoped to the authenticated ride participants; refunds require `ADMIN`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/fares/estimate` | Save a fare estimate |
| `GET` | `/api/v1/fares/{id}` | Retrieve a fare estimate |
| `POST` | `/api/v1/payments` | Verify a completed ride and process an explicitly initiated simulated payment |
| `GET` | `/api/v1/payments/{id}` | Retrieve a payment |
| `GET` | `/api/v1/payments/ride/{rideId}` | Retrieve payment records for a ride |
| `GET` | `/api/v1/payments/{id}/receipt` | Retrieve a receipt |
| `POST` | `/api/v1/payments/{id}/refund` | Admin refund simulation |

## Commands

Run in PowerShell from `fare-payment-service`:

```powershell
.\gradlew.bat check
.\gradlew.bat bootRun
```

`check` runs tests and Checkstyle. For the complete end-to-end workflow, see the root [RideLink README](../README.md).

Payment amounts are stored as integer cents (100 cents = LKR 1). The fare calculator uses LKR 200 base, LKR 80/km, LKR 10/minute, an optional 1.0–2.5 surge, and LKR 300 minimum.

## Delivery limitation

Ride Management uses a PostgreSQL outbox and waits for RabbitMQ publisher confirms before marking events as published. Fare & Payment uses broker retries and a dead-letter queue; the database's unique `ride_id` constraint makes duplicate delivery safe. If a message reaches the dead-letter queue, an operator must investigate and replay it after correcting the cause. This setup demonstrates reliable single-instance delivery, not payment gateway settlement or distributed tracing/metrics.
