package com.ridelink.drivervehicle.controller;

import com.ridelink.drivervehicle.dto.request.VehicleRequestDto;
import com.ridelink.drivervehicle.dto.response.ApiResponse;
import com.ridelink.drivervehicle.dto.response.VehicleResponseDto;
import com.ridelink.drivervehicle.service.VehicleService;
import com.ridelink.drivervehicle.service.ResourceOwnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;
    private final ResourceOwnershipService ownership;

    /**
     * POST /api/vehicles
     * Create vehicle details
     */
    @PostMapping
    public ResponseEntity<VehicleResponseDto> createVehicle(@Valid @RequestBody VehicleRequestDto request) {
        log.info("REST request to create vehicle with plate number: {}", request.getPlateNumber());
        VehicleResponseDto createdVehicle = vehicleService.createVehicle(request);
        return new ResponseEntity<>(createdVehicle, HttpStatus.CREATED);
    }

    /**
     * GET /api/vehicles
     * Get all vehicles
     */
    @GetMapping
    public ResponseEntity<List<VehicleResponseDto>> getAllVehicles() {
        log.info("REST request to get all vehicles");
        List<VehicleResponseDto> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }

    /**
     * GET /api/vehicles/{id}
     * Get vehicle by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDto> getVehicleById(@PathVariable Long id, Authentication authentication) {
        ownership.requireVehicleOwner(id, authentication);
        log.info("REST request to get vehicle with ID: {}", id);
        VehicleResponseDto vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * PUT /api/vehicles/{id}
     * Update vehicle details by ID
     */
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDto> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequestDto request) {
        log.info("REST request to update vehicle with ID: {}", id);
        VehicleResponseDto updatedVehicle = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(updatedVehicle);
    }

    /**
     * DELETE /api/vehicles/{id}
     * Delete vehicle by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Long id) {
        log.info("REST request to delete vehicle with ID: {}", id);
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle with ID " + id + " has been successfully deleted"));
    }

    /**
     * PUT /api/vehicles/{id}/assign/{driverId}
     * Assign vehicle to a specific driver
     */
    @PutMapping("/{id}/assign/{driverId}")
    public ResponseEntity<VehicleResponseDto> assignVehicleToDriver(
            @PathVariable Long id,
            @PathVariable Long driverId) {
        log.info("REST request to assign vehicle ID: {} to driver ID: {}", id, driverId);
        VehicleResponseDto vehicle = vehicleService.assignVehicleToDriver(id, driverId);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * GET /api/vehicles/driver/{driverId}
     * Get all vehicles belonging to a specific driver
     */
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<VehicleResponseDto>> getVehiclesByDriverId(
            @PathVariable Long driverId, Authentication authentication) {
        ownership.requireDriverOwner(driverId, authentication);
        log.info("REST request to get vehicles for driver ID: {}", driverId);
        List<VehicleResponseDto> vehicles = vehicleService.getVehiclesByDriverId(driverId);
        return ResponseEntity.ok(vehicles);
    }
}
