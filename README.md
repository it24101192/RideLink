# 🚖 RideLink — Microservices Platform

RideLink is a distributed, microservices-based ride-hailing and transport management platform built with Java 21 and Spring Boot.

---

## 👥 Microservice Responsibilities Breakdown

| Member | Microservice | Responsibility | Technology | Port |
|---|---|---|---|---|
| **Member 1** | **Account Service** | Authentication, Registration, Login, JWT tokens, Role Management | Spring Boot, Gradle, MySQL | `8080` / `8081` |
| **Member 2** | **Driver & Vehicle Service** | Driver operational profiles, Vehicle management, Availability status (`AVAILABLE`/`BUSY`/`OFFLINE`), Service areas, Simulated location, Eligible driver discovery for Ride Management | Spring Boot 3.2.5, Maven, JPA, MySQL | `8082` |
| **Member 3** | **Ride Management Service** | Ride requests, driver matching, ride lifecycle, pickup/destination tracking | Spring Boot | `8083` |
| **Member 4** | **Fare & Payment Service** | Fare calculation, payment processing, transaction receipts | Spring Boot | `8084` |

---

## 🚗 Member 2: Driver & Vehicle Service Details
See [`driver-vehicle-service/README.md`](./driver-vehicle-service/README.md) for full endpoint specifications, schema, running instructions, and integration workflows.

### Postman Collection
Import the collection located at:
`./RideLink_Driver_Vehicle_Service.postman_collection.json`