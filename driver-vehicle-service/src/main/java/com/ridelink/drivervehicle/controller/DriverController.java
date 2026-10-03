package com.ridelink.drivervehicle.controller;

import com.ridelink.drivervehicle.dto.request.AvailabilityUpdateDto;
import com.ridelink.drivervehicle.dto.request.DriverRequestDto;
import com.ridelink.drivervehicle.dto.request.LocationUpdateDto;
import com.ridelink.drivervehicle.dto.response.ApiResponse;
import com.ridelink.drivervehicle.dto.response.AvailableDriverResponseDto;
import com.ridelink.drivervehicle.dto.response.DriverResponseDto;
import com.ridelink.drivervehicle.dto.response.EligibleDriverResponseDto;
import com.ridelink.drivervehicle.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DriverController {

    private final DriverService driverService;

    /**
     * POST /api/drivers
     * Create driver operational profile
     */
    @PostMapping
    public ResponseEntity<DriverResponseDto> createDriver(@Valid @RequestBody DriverRequestDto request) {
        log.info("REST request to create driver profile: {}", request.getName());
        DriverResponseDto response = driverService.registerDriver(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/drivers
     * Retrieve all drivers
     */
    @GetMapping
    public ResponseEntity<List<DriverResponseDto>> getAllDrivers() {
        log.info("REST request to get all drivers");
        List<DriverResponseDto> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(drivers);
    }

    /**
     * GET /api/drivers/{id}
     * Retrieve driver operational profile by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponseDto> getDriverById(@PathVariable Long id) {
        log.info("REST request to get driver profile with ID: {}", id);
        DriverResponseDto driver = driverService.getDriverById(id);
        return ResponseEntity.ok(driver);
    }

    /**
     * PUT /api/drivers/{id}
     * Update driver profile
     */
    @PutMapping("/{id}")
    public ResponseEntity<DriverResponseDto> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverRequestDto request) {
        log.info("REST request to update driver profile with ID: {}", id);
        DriverResponseDto updatedDriver = driverService.updateDriver(id, request);
        return ResponseEntity.ok(updatedDriver);
    }

    /**
     * DELETE /api/drivers/{id}
     * Delete driver profile by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable Long id) {
        log.info("REST request to delete driver with ID: {}", id);
        driverService.deleteDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Driver with ID " + id + " has been successfully deleted"));
    }

    /**
     * PUT /api/drivers/{id}/availability (also supports PATCH)
     * Update driver availability status (AVAILABLE, BUSY, OFFLINE)
     */
    @RequestMapping(value = "/{id}/availability", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<DriverResponseDto> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityUpdateDto request) {
        log.info("REST request to update availability for driver ID: {} to {}", id, request.getAvailability());
        DriverResponseDto response = driverService.updateAvailability(id, request.getAvailability());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/drivers/{id}/location (also supports PATCH)
     * Update simulated driver current location
     */
    @RequestMapping(value = "/{id}/location", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<DriverResponseDto> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationUpdateDto request) {
        log.info("REST request to update location for driver ID: {} to ({}, {})", id, request.getLatitude(), request.getLongitude());
        DriverResponseDto response = driverService.updateLocation(id, request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/drivers/available
     * Retrieve all available drivers (availability = AVAILABLE)
     */
    @GetMapping("/available")
    public ResponseEntity<List<DriverResponseDto>> getAvailableDrivers() {
        log.info("REST request to get all available drivers");
        List<DriverResponseDto> availableDrivers = driverService.getAvailableDrivers();
        return ResponseEntity.ok(availableDrivers);
    }

    /**
     * GET /api/drivers/available?lat=6.9&lng=79.8&radius=10
     * API for Member 3 (Ride Management Service) DriverServiceClient.
     * Returns AVAILABLE drivers within radius km (default 10), nearest first,
     * as [{ id, lat, lng, serviceArea, ... }] where id is the Account Service user id.
     */
    @GetMapping(value = "/available", params = {"lat", "lng"})
    public ResponseEntity<List<AvailableDriverResponseDto>> getNearbyAvailableDrivers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius) {
        log.info("REST request to get available drivers near ({}, {}) within {} km", lat, lng, radius);
        return ResponseEntity.ok(driverService.getNearbyAvailableDrivers(lat, lng, radius));
    }

    /**
     * GET /api/drivers/eligible?serviceArea=Colombo
     * API for Member 3 (Ride Management Service) to retrieve eligible available drivers
     * (AVAILABLE, matching serviceArea, and assigned to an active vehicle).
     */
    @GetMapping("/eligible")
    public ResponseEntity<List<EligibleDriverResponseDto>> getEligibleDrivers(
            @RequestParam(required = false) String serviceArea) {
        log.info("REST request to get eligible available drivers for service area: {}", serviceArea);
        List<EligibleDriverResponseDto> eligibleDrivers = driverService.getEligibleAvailableDrivers(serviceArea);
        return ResponseEntity.ok(eligibleDrivers);
    }
}
