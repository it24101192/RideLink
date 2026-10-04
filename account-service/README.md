# Account Service

Account Service owns registration, password hashing, login, account roles/status, UUID identities, and signed JWTs. Other services validate the same JWT using the shared HMAC `JWT_SECRET`; Ride Management also calls `GET /api/users/{userId}` to check that an account is active and has the required role.

## Local setup

The service listens on port `8081` and uses MySQL schema `ridelink_account_db` by default. For the full stack, configure the repository root `.env` and run `docker compose up --build -d`. For IDE execution, configure `ACCOUNT_DB_URL`, `ACCOUNT_DB_USERNAME`, `ACCOUNT_DB_PASSWORD`, and `JWT_SECRET`.

`JWT_SECRET` must be at least 32 bytes and must match the value used by Driver & Vehicle, Ride Management, and Fare & Payment. Do not commit the secret.

Run checks from this directory:

```powershell
.\gradlew.bat check
.\gradlew.bat bootRun
```

Health is available at `http://localhost:8081/health`.

## First administrator

Public account registration always creates a `PASSENGER`; it cannot set an arbitrary role. To provision the initial administrator for local development, set `ADMIN_BOOTSTRAP_USERNAME`, `ADMIN_BOOTSTRAP_EMAIL`, and `ADMIN_BOOTSTRAP_PASSWORD` together before starting the service. The password must be at least 12 characters. Account Service hashes it, creates the admin only when the username is not already present, and refuses to promote an existing non-admin account. The credentials are not exposed through a public endpoint. Keep them private and consistent while using the local database volume.

After startup, log in at `POST /api/accounts/login` to obtain the ADMIN JWT. Use that token to create driver accounts at `POST /api/accounts/admin/drivers` and to perform Ride Management's admin-only assignment.

## Main endpoints

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/accounts/register` | Public | Register a passenger account |
| `POST` | `/api/accounts/login` | Public | Obtain JWT and account UUID |
| `POST` | `/api/accounts/admin/drivers` | ADMIN | Create driver account |
| `GET` | `/api/users/{userId}` | Authenticated | Return role/status for downstream identity validation |
| `GET` / `PUT` | `/api/accounts/profile` | Authenticated | Read/update current user's profile |
| `PATCH` | `/api/accounts/profile/deactivate` | Authenticated | Deactivate current account |

The account UUID returned at registration/login is used as the driver profile's `accountId` and as Ride Management's passenger/driver identity. See the root [RideLink README](../README.md) for the four-service flow and [end-to-end smoke script](../e2e-smoke-test.ps1).
