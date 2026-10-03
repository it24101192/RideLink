# 🚗 RideLink — Member 2: Driver & Vehicle Microservice

## 📌 1. Service Responsibility & Overview
The **Driver & Vehicle Service** is a dedicated microservice in the **RideLink** platform responsible for managing:
1. **Driver Operational Profiles** (Driver bio, contact, license, external reference to Account Service via `accountId`).
2. **Vehicle Details & Assignment** (Vehicle model, type, registration plate number, driver-vehicle binding).
3. **Driver Availability State Management** (`AVAILABLE`, `BUSY`, `OFFLINE`).
4. **Service Area Management** (City/region assignment such as *Colombo*, *Kandy*, *Galle*, *Jaffna*).
5. **Simulated Real-Time Location Tracking** (Latitude, longitude, `locationUpdatedAt` timestamp).
6. **Eligible Available Driver Retrieval for Ride Management Service (Member 3)**.

> 🔒 **Microservice Boundary Compliance:**
> - **Account Service (Member 1)** manages user registration, login, password encryption, and JWT authentication.
> - **Driver & Vehicle Service (Member 2)** references accounts via `accountId` without duplicating auth/credential data.
> - **Ride Management Service (Member 3)** consumes Driver & Vehicle Service REST APIs to find available drivers for trip requests.
> - **Database starts empty:** Zero seed/dummy data or CommandLineRunners are embedded. Data is populated exclusively via REST APIs / Postman.

---

## 🛠️ 2. Technology Stack
- **Java:** `21` (LTS)
- **Framework:** `Spring Boot 4.0.8` (same as Account, Ride Management and Fare & Payment services)
- **Persistence:** `Spring Data JPA` / `Hibernate`
- **Database:** `MySQL 8.x` (Database name: `ridelink_driver_db`)
- **Testing:** `JUnit 5`, `MockMvc`, `H2 In-Memory DB` (for tests)
- **Build Tool:** `Gradle` (`gradlew`, Gradle 9.7.1 — same wrapper as the other services)
- **API Tooling:** `Postman Collection v2.1`

---

## 🗄️ 3. Database Schema & Architecture

### **Database Name:** `ridelink_driver_db`

### Table: `drivers`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique Driver ID |
| `account_id` | `BIGINT` | `NULLABLE` | Reference to Member 1 Account Service `id` |
| `name` | `VARCHAR(100)` | `NOT NULL` | Driver Full Name |
| `phone` | `VARCHAR(20)` | `NOT NULL` | Contact Number |
| `email` | `VARCHAR(100)` | `NULLABLE`, `UNIQUE` | Driver Email |
| `license_no` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | Driving License Number |
| `availability` | `VARCHAR(20)` | `NOT NULL`, Default `AVAILABLE` | Enum: `AVAILABLE`, `BUSY`, `OFFLINE` |
| `service_area` | `VARCHAR(100)` | `NULLABLE` | Operating Area/City (e.g. `Colombo`) |
| `latitude` | `DOUBLE` | `NULLABLE` | Simulated Current Latitude |
| `longitude` | `DOUBLE` | `NULLABLE` | Simulated Current Longitude |
| `location_updated_at` | `DATETIME` | `NULLABLE` | Timestamp of last location update |

### Table: `vehicles`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique Vehicle ID |
| `driver_id` | `BIGINT` | `NULLABLE` | Assigned Driver ID |
| `vehicle_type` | `VARCHAR(50)` | `NOT NULL` | Vehicle Category (e.g. `CAR`, `VAN`, `BIKE`) |
| `model` | `VARCHAR(100)` | `NOT NULL` | Make & Model (e.g. `Toyota Prius`) |
| `plate_number` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | License Plate Number (e.g. `CAB-1234`) |

---

## 🌐 4. REST API Reference

### 🧑‍✈️ Driver Endpoints (`/api/drivers`)
| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| `POST` | `/api/drivers` | Create driver operational profile | `201 Created`, `400`, `409` |
| `GET` | `/api/drivers` | Retrieve all drivers | `200 OK` |
| `GET` | `/api/drivers/{id}` | Get driver profile by ID | `200 OK`, `404 Not Found` |
| `PUT` | `/api/drivers/{id}` | Update driver operational profile | `200 OK`, `400`, `404`, `409` |
| `DELETE` | `/api/drivers/{id}` | Delete driver and cascade delete vehicle | `200 OK`, `404 Not Found` |

### 🚦 Availability & Location Endpoints
| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| `PUT` / `PATCH` | `/api/drivers/{id}/availability` | Update availability status (`AVAILABLE`, `BUSY`, `OFFLINE`) | `200 OK`, `400`, `404` |
| `PUT` / `PATCH` | `/api/drivers/{id}/location` | Update simulated location (`latitude`, `longitude`) | `200 OK`, `400`, `404` |

### 🔍 Driver Discovery Endpoints
| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| `GET` | `/api/drivers/available` | Retrieve all drivers who are currently `AVAILABLE` | `200 OK` |
| `GET` | `/api/drivers/available?lat={lat}&lng={lng}&radius={km}` | Nearby `AVAILABLE` drivers for Ride Management Service (nearest first, default radius 10 km) | `200 OK` |
| `GET` | `/api/drivers/eligible?serviceArea={area}` | Retrieve eligible available drivers for Ride Management Service | `200 OK` |

### 🚘 Vehicle Endpoints (`/api/vehicles`)
| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| `POST` | `/api/vehicles` | Create vehicle | `201 Created`, `400`, `404`, `409` |
| `GET` | `/api/vehicles` | Retrieve all vehicles | `200 OK` |
| `GET` | `/api/vehicles/{id}` | Get vehicle by ID | `200 OK`, `404 Not Found` |
| `PUT` | `/api/vehicles/{id}` | Update vehicle details | `200 OK`, `400`, `404`, `409` |
| `DELETE` | `/api/vehicles/{id}` | Delete vehicle by ID | `200 OK`, `404 Not Found` |
| `PUT` | `/api/vehicles/{id}/assign/{driverId}` | Assign vehicle to a specific driver | `200 OK`, `404 Not Found` |
| `GET` | `/api/vehicles/driver/{driverId}` | Get all vehicles owned/assigned to driver | `200 OK`, `404 Not Found` |

---

## 🔄 5. Inter-Service Integration Workflow

### 🔗 Integration with Account Service (Member 1)
- When a driver registers an account through Member 1's Account Service (`POST /api/accounts/register`), an `accountId` (e.g. `101`) is generated.
- In Driver & Vehicle Service, the driver's operational profile is registered using `POST /api/drivers` containing `"accountId": 101`.
- Authentication and passwords remain exclusively within Member 1.

### 🚕 Integration with Ride Management Service (Member 3)

#### Nearby driver lookup (used by `ride-management-service` `DriverServiceClient`)
```http
GET http://localhost:8082/api/drivers/available?lat=6.9271&lng=79.8612&radius=10
```
Returns `AVAILABLE` drivers that have an `accountId` and a known location inside the radius, nearest first:
```json
[
  { "id": "101", "driverId": 1, "lat": 6.9271, "lng": 79.8612, "serviceArea": "Colombo", "distanceKm": 0.0 }
]
```
`id` is the driver's **Account Service** user id (Ride Management stores it as the ride's `driverId`).

> ⚠️ Ride Management Service defaults `DRIVER_SERVICE_URL` to `http://localhost:3002`. Start it with
> `DRIVER_SERVICE_URL=http://localhost:8082`. It also currently parses driver ids as UUIDs, while
> Account Service issues numeric ids — this must be aligned by the Ride Management owner.

#### Eligible drivers by service area
When a passenger requests a ride in **Colombo**, Member 3 can call:
```http
GET http://localhost:8082/api/drivers/eligible?serviceArea=Colombo
```

#### Selection Criteria:
1. Driver's availability is **`AVAILABLE`**.
2. Driver's `serviceArea` matches **`Colombo`** (case-insensitive).
3. Driver is assigned an **active vehicle**.

#### Response Payload format:
```json
[
  {
    "driverId": 1,
    "accountId": 101,
    "driverName": "Kamal Perera",
    "vehicleId": 1,
    "vehicleNumber": "CAB-1234",
    "vehicleType": "CAR",
    "availability": "AVAILABLE",
    "serviceArea": "Colombo",
    "latitude": 6.9271,
    "longitude": 79.8612,
    "locationUpdatedAt": "2026-10-01T20:45:00"
  }
]
```

---

## 🚀 6. How to Run the Service

### Prerequisites
1. **Java 21 JDK** installed and set in `PATH`.
2. **MySQL Server** running on `localhost:3306`.
3. Database `ridelink_driver_db` (will be auto-created if configured with `createDatabaseIfNotExist=true`).

### Step 1: Configure Database Credentials (Optional)
If your MySQL password is not blank or root, set the environment variable:
```powershell
$env:RIDELINK_DB_PASSWORD="your_mysql_password"
```

### Step 2: Build and Run
In the `driver-vehicle-service` directory:
```powershell
# Run tests
.\gradlew.bat test

# Run Spring Boot Application (Starts on Port 8082)
.\gradlew.bat bootRun
```

From the repository root (as CI does): `bash driver-vehicle-service/gradlew -p driver-vehicle-service clean test build`

The service will start on: `http://localhost:8082`

---

## 🧪 7. Testing with Postman

A pre-configured Postman Collection is provided:
📁 `RideLink_Driver_Vehicle_Service.postman_collection.json`

### Import Instructions:
1. Open **Postman**.
2. Click **Import** (Top Left) -> Select `RideLink_Driver_Vehicle_Service.postman_collection.json`.
3. Set the environment variable `baseUrl` to `http://localhost:8082` (configured by default).

### Recommended Testing Sequence:
1. **Driver 1 Registration**: Kamal Perera (Colombo, Account ID `101`, `AVAILABLE`)
2. **Driver 2 Registration**: Nimal Fernando (Colombo, Account ID `102`, `AVAILABLE`)
3. **Driver 3 Registration**: Ravi Shanmugam (Kandy, Account ID `103`, `AVAILABLE`)
4. **Vehicle Creation**:
   - `CAB-1234` (Toyota Prius, CAR) -> assigned to Kamal (Driver 1)
   - `WP-CAR-5678` (Honda Grace, CAR) -> assigned to Nimal (Driver 2)
   - `CP-VAN-9012` (Toyota HiAce, VAN) -> assigned to Ravi (Driver 3)
5. **State Transitions**:
   - Update Nimal to `BUSY` via `PUT /api/drivers/2/availability`
   - Update Ravi to `OFFLINE` via `PUT /api/drivers/3/availability`
6. **Location Simulation**:
   - Update Kamal's coordinates: `6.9271, 79.8612` (Galle Face, Colombo)
7. **Eligible Driver Query**:
   - Query `GET /api/drivers/eligible?serviceArea=Colombo` -> Returns **Kamal Perera** with Vehicle `CAB-1234`.
   - Nimal is excluded because status is `BUSY`.
   - Ravi is excluded because service area is `Kandy`.
