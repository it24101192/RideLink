package com.ridelink.ridemanagement.controller;

import com.ridelink.ridemanagement.api.RideDtos;
import com.ridelink.ridemanagement.api.RideRequests;
import com.ridelink.ridemanagement.model.Ride;
import com.ridelink.ridemanagement.model.RidePayment;
import com.ridelink.ridemanagement.model.RideStatus;
import com.ridelink.ridemanagement.security.AuthenticatedUser;
import com.ridelink.ridemanagement.service.RideService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rides")
public class RideController {
    private final RideService service;
    public RideController(RideService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<RideDtos.RideResponse> create(@Valid @RequestBody RideRequests.CreateRideRequest request,
        Authentication authentication, @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        Ride ride = service.create(request, AuthenticatedUser.from(authentication), bearer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response(ride));
    }
    @GetMapping("/{id}")
    public RideDtos.RideResponse get(@PathVariable UUID id, Authentication authentication) {
        return response(service.get(id, AuthenticatedUser.from(authentication)));
    }
    @GetMapping
    public List<RideDtos.RideResponse> list(@RequestParam(required = false) UUID passengerId,
        @RequestParam(required = false) UUID driverId, @RequestParam(required = false) RideStatus status,
        Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUser.from(authentication);
        return service.list(user, passengerId, driverId, status).stream().map(this::response).toList();
    }
    @PostMapping("/{id}/assign")
    public RideDtos.RideResponse assign(@PathVariable UUID id, @RequestBody(required = false) RideRequests.AssignRequest request,
        Authentication authentication, @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        Ride ride = request == null ? service.assignNearest(id, AuthenticatedUser.from(authentication), bearer)
            : service.assign(id, request, AuthenticatedUser.from(authentication), bearer);
        return response(ride);
    }
    @PostMapping("/{id}/accept")
    public RideDtos.RideResponse accept(@PathVariable UUID id, Authentication authentication,
        @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        return response(service.accept(id, AuthenticatedUser.from(authentication), bearer));
    }
    @PostMapping("/{id}/start")
    public RideDtos.RideResponse start(@PathVariable UUID id, Authentication authentication) {
        return response(service.start(id, AuthenticatedUser.from(authentication)));
    }
    @PostMapping("/{id}/complete")
    public RideDtos.RideResponse complete(@PathVariable UUID id, Authentication authentication) {
        return response(service.complete(id, AuthenticatedUser.from(authentication)));
    }
    @PostMapping("/{id}/cancel")
    public RideDtos.RideResponse cancel(@PathVariable UUID id, @Valid @RequestBody RideRequests.CancelRequest request,
        Authentication authentication) {
        return response(service.cancel(id, request, AuthenticatedUser.from(authentication)));
    }
    @GetMapping("/fare-estimate")
    public RideDtos.FareEstimateResponse estimate(@RequestParam double pickupLat,
        @RequestParam double pickupLng, @RequestParam double destLat, @RequestParam double destLng,
        @RequestParam(defaultValue = "30") int durationMin) {
        return service.estimate(pickupLat, pickupLng, destLat, destLng, durationMin);
    }
    @GetMapping("/{id}/fare-estimate")
    public RideDtos.FareEstimateResponse estimateExisting(@PathVariable UUID id, Authentication authentication) {
        return service.estimate(id, AuthenticatedUser.from(authentication));
    }
    @GetMapping("/{id}/receipt")
    public RideDtos.ReceiptResponse receipt(@PathVariable UUID id, Authentication authentication) {
        RidePayment payment = service.receipt(id, AuthenticatedUser.from(authentication));
        return RideDtos.ReceiptResponse.from(payment);
    }
    private RideDtos.RideResponse response(Ride ride) {
        return RideDtos.RideResponse.from(ride, service.history(ride.getId()));
    }
}
