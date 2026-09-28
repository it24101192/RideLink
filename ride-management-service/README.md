# Ride Management Service — RideLink

Owner: Member 3 (per IT3130 group assignment brief)

## Tech stack
- Java 21
- Spring Boot 3.3.x (Web, Data JPA, Validation, Security)
- MySQL (own schema: `ride_db`)
- springdoc-openapi (Swagger UI)

## Prerequisites
- JDK 21
- Maven 3.9+
- MySQL Server running locally (or reachable via env vars below)

## Configuration (environment variables)
| Variable | Purpose | Default |
|---|---|---|
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | *(empty)* |
| `DRIVER_SERVICE_URL` | Base URL of Driver & Vehicle Service | `http://localhost:8082` |
| `FARE_PAYMENT_SERVICE_URL` | Base URL of Fare & Payment Service | `http://localhost:8084` |
| `ACCOUNT_SERVICE_URL` | Base URL of Account Service | `http://localhost:8081` |

Never commit real credentials — set these via your shell, an `.env` file (gitignored), or your CI secrets store.

## Start-up order
1. Start MySQL.
2. Start **Account Service** (8081).
3. Start **Driver & Vehicle Service** (8082).
4. Start **this service** (8083): `mvn spring-boot:run`
5. Start **Fare & Payment Service** (8084).

## Running
```
mvn clean install
mvn spring-boot:run
```
Service runs on `http://localhost:8083`.

## API docs
Swagger UI: `http://localhost:8083/swagger-ui.html`
OpenAPI JSON: `http://localhost:8083/v3/api-docs`

## Running tests
```
mvn test
```

## Endpoints
| Method | Path | Description |
|---|---|---|
| POST | `/api/rides` | Create a ride request |
| GET | `/api/rides/{id}` | Get ride by id |
| GET | `/api/rides?passengerId=` | List rides for a passenger |
| GET | `/api/rides?driverId=` | List rides for a driver |
| PUT | `/api/rides/{id}/assign` | Assign a driver |
| PUT | `/api/rides/{id}/accept` | Driver accepts |
| PUT | `/api/rides/{id}/start` | Start ride |
| PUT | `/api/rides/{id}/complete` | Complete ride (triggers fare finalization) |
| PUT | `/api/rides/{id}/cancel` | Cancel ride |

## Ride status lifecycle
```
REQUESTED -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
    |            |           |
    +-------> CANCELLED <----+
```
Illegal transitions (e.g. COMPLETED -> anything) are rejected with HTTP 409
and error code `INVALID_STATUS_TRANSITION`.

## Interservice communication
- **Driver & Vehicle Service** (sync REST): fetch available drivers before
  confirming assignment — real-time accuracy needed, so synchronous.
- **Fare & Payment Service** (sync REST): fare estimate on creation, final
  fare trigger on completion.
- **Account Service**: JWT issued by Account Service is validated by this
  service's security filter (see `SecurityConfig`, TODO for full JWT wiring).

## Negative scenarios demonstrated
1. `NO_DRIVER_AVAILABLE` (409) — assigning a driver when none are available.
2. `INVALID_STATUS_TRANSITION` (409) — e.g. completing a ride still in
   `REQUESTED` state.

## Sample test data
| passengerId | pickup | destination |
|---|---|---|
| 1 | Colombo 03 | Bandaranaike International Airport |
| 2 | Kandy | Nuwara Eliya |
