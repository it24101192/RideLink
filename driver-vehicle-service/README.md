# Driver & Vehicle Service

Driver & Vehicle owns driver operational profiles, vehicle records, availability, location, and driver discovery. It stores the Account Service account UUID on each driver profile and never stores account credentials.

## Run with all RideLink services

From the repository root, configure `.env` from `.env.example` and start the complete stack:

```powershell
Copy-Item .env.example .env
# Set private local passwords and the shared JWT_SECRET in .env.
docker compose up --build -d
```

The service listens on port `3002`; health is `http://localhost:3002/health`. In Docker, its database hostname is `mysql`. For IDE execution, configure a MySQL database named `ridelink_driver_db`, set `JWT_SECRET` to the same 32+ byte HMAC secret as the other services, and set `DRIVER_DB_URL`, `DRIVER_DB_USERNAME`, and `DRIVER_DB_PASSWORD`.

Run checks from this directory:

```powershell
.\gradlew.bat check
.\gradlew.bat bootRun
```

## Authentication and access

Every `/api/**` endpoint requires a valid Account Service JWT. Driver & Vehicle validates that token with the shared `JWT_SECRET` and maps `RIDER` to `PASSENGER`. Profile/vehicle provisioning, deletion, and driver discovery require `ADMIN`. Drivers can read their own operational profile and assigned vehicles, and update their own availability/location. A driver cannot read or update another driver's profile. The service's CORS origins are configured by `APP_CORS_ALLOWED_ORIGINS`.

Create an account in Account Service, log in, then use the returned account UUID as `accountId` when an admin creates a driver profile. Do not use the Driver database's numeric internal driver row ID as the Ride Management driver identity; Ride Management uses the Account UUID.

## REST endpoints

Base URL: `http://localhost:3002`; API prefix: `/api`.

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/drivers` | ADMIN | Provision operational profile for an Account UUID |
| `GET` | `/api/drivers` | ADMIN | List driver profiles |
| `GET` | `/api/drivers/{id}` | Owning DRIVER or ADMIN | Read operational profile (`id` is the local numeric driver row ID) |
| `PUT` | `/api/drivers/{id}` | ADMIN | Update operational profile |
| `DELETE` | `/api/drivers/{id}` | ADMIN | Delete profile |
| `PUT` / `PATCH` | `/api/drivers/{id}/availability` | Owning DRIVER or ADMIN | Update availability |
| `PUT` / `PATCH` | `/api/drivers/{id}/location` | Owning DRIVER or ADMIN | Update location |
| `GET` | `/api/drivers/available?lat={lat}&lng={lng}&radius={km}` | ADMIN | Nearby available drivers for Ride Management |
| `GET` | `/api/drivers/eligible?serviceArea={area}` | ADMIN | Available drivers with an eligible vehicle |
| `POST` | `/api/vehicles` | ADMIN | Create vehicle |
| `GET` | `/api/vehicles` | ADMIN | List vehicles |
| `GET` | `/api/vehicles/{id}` | Owning DRIVER or ADMIN | Read an assigned vehicle |
| `PUT` / `DELETE` | `/api/vehicles/{id}` | ADMIN | Update or delete vehicle |
| `PUT` | `/api/vehicles/{id}/assign/{driverId}` | ADMIN | Assign a vehicle |
| `GET` | `/api/vehicles/driver/{driverId}` | Owning DRIVER or ADMIN | List assigned vehicles |

Ride Management forwards its admin Bearer token to the nearby-driver endpoint. The response includes `id` (Account UUID), `driverId` (local profile row ID), coordinates, `serviceArea`, and distance. It uses the Account UUID for ride assignment.

The matching rule in Ride Management is controlled by `DRIVER_SERVICE_AREA_CHECK` (default `false`). When enabled, include the requested `serviceArea` on ride creation; assignment requires a case-insensitive exact match.

## Postman and tests

Import `RideLink_Driver_Vehicle_Service.postman_collection.json`. Set `baseUrl` to `http://localhost:3002` and provide an admin or owning-driver Bearer token according to the endpoint access above. The collection contains examples; the service starts with an empty database.

For the multi-service flow, see the repository root [README](../README.md) and [end-to-end smoke script](../e2e-smoke-test.ps1).
