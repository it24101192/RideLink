package com.ridelink.ridemanagement.controller;

import com.ridelink.ridemanagement.dto.AssignDriverRequest;
import com.ridelink.ridemanagement.dto.CancelRideRequest;
import com.ridelink.ridemanagement.dto.CreateRideRequest;
import com.ridelink.ridemanagement.dto.RideResponse;
import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rides")
@Tag(name = "Ride Management", description = "Ride request, assignment and lifecycle operations")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @Operation(summary = "Create a new ride request")
    @PostMapping
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request) {
        Ride ride = rideService.createRide(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RideResponse.fromEntity(ride));
    }

    @Operation(summary = "Get ride details by id")
    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRide(@PathVariable Long id) {
        return ResponseEntity.ok(RideResponse.fromEntity(rideService.getRide(id)));
    }

    @Operation(summary = "List rides, optionally filtered by passenger or driver")
    @GetMapping
    public ResponseEntity<List<RideResponse>> listRides(
            @RequestParam(required = false) Long passengerId,
            @RequestParam(required = false) Long driverId) {

        List<Ride> rides;
        if (passengerId != null) {
            rides = rideService.getRidesByPassenger(passengerId);
        } else if (driverId != null) {
            rides = rideService.getRidesByDriver(driverId);
        } else {
            rides = List.of();
        }
        return ResponseEntity.ok(rides.stream().map(RideResponse::fromEntity).toList());
    }

    @Operation(summary = "Assign a driver to a ride request")
    @PutMapping("/{id}/assign")
    public ResponseEntity<RideResponse> assignDriver(@PathVariable Long id,
                                                       @Valid @RequestBody AssignDriverRequest request) {
        Ride ride = rideService.assignDriver(id, request);
        return ResponseEntity.ok(RideResponse.fromEntity(ride));
    }

    @Operation(summary = "Driver accepts an assigned ride")
    @PutMapping("/{id}/accept")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable Long id) {
        return ResponseEntity.ok(RideResponse.fromEntity(rideService.acceptRide(id)));
    }

    @Operation(summary = "Driver starts the ride")
    @PutMapping("/{id}/start")
    public ResponseEntity<RideResponse> startRide(@PathVariable Long id) {
        return ResponseEntity.ok(RideResponse.fromEntity(rideService.startRide(id)));
    }

    @Operation(summary = "Mark the ride as completed")
    @PutMapping("/{id}/complete")
    public ResponseEntity<RideResponse> completeRide(@PathVariable Long id) {
        return ResponseEntity.ok(RideResponse.fromEntity(rideService.completeRide(id)));
    }

    @Operation(summary = "Cancel a ride")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable Long id,
                                                     @RequestBody(required = false) CancelRideRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(RideResponse.fromEntity(rideService.cancelRide(id, reason)));
    }
}
