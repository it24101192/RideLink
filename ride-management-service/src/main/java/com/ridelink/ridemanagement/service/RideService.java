package com.ridelink.ridemanagement.service;

import com.ridelink.ridemanagement.api.RideDtos;
import com.ridelink.ridemanagement.api.RideRequests;
import com.ridelink.ridemanagement.client.AccountServiceClient;
import com.ridelink.ridemanagement.domain.FareCalculator;
import com.ridelink.ridemanagement.domain.StatusTransitionValidator;
import com.ridelink.ridemanagement.error.DomainException;
import com.ridelink.ridemanagement.messaging.RideCompletedEventPublisher;
import com.ridelink.ridemanagement.model.Ride;
import com.ridelink.ridemanagement.model.RidePayment;
import com.ridelink.ridemanagement.model.RideStatus;
import com.ridelink.ridemanagement.model.RideStatusHistory;
import com.ridelink.ridemanagement.repository.RidePaymentRepository;
import com.ridelink.ridemanagement.repository.RideRepository;
import com.ridelink.ridemanagement.repository.RideStatusHistoryRepository;
import com.ridelink.ridemanagement.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideService {
    private final RideRepository rides;
    private final RideStatusHistoryRepository history;
    private final RidePaymentRepository payments;
    private final FareCalculator fareCalculator;
    private final StatusTransitionValidator transitions;
    private final DriverAssignmentService assignment;
    private final AccountServiceClient accounts;
    private final PaymentService paymentService;
    private final RideCompletedEventPublisher events;
    private final RideCompletionService completionService;

    public RideService(RideRepository rides, RideStatusHistoryRepository history, RidePaymentRepository payments,
                       FareCalculator fareCalculator, StatusTransitionValidator transitions,
                       DriverAssignmentService assignment, AccountServiceClient accounts,
                       PaymentService paymentService, RideCompletedEventPublisher events,
                       RideCompletionService completionService) {
        this.rides = rides; this.history = history; this.payments = payments; this.fareCalculator = fareCalculator;
        this.transitions = transitions; this.assignment = assignment; this.accounts = accounts;
        this.paymentService = paymentService; this.events = events;
        this.completionService = completionService;
    }

    @Transactional
    public Ride create(RideRequests.CreateRideRequest request, AuthenticatedUser user, String bearerToken) {
        requireRole(user, "PASSENGER"); accounts.validateUser(user.userId(), "PASSENGER", bearerToken);
        double distance = fareCalculator.distanceKm(request.pickupLat(), request.pickupLng(), request.destLat(), request.destLng());
        int duration = request.durationMin() == null ? Math.max(1, (int) Math.ceil(distance * 3)) : request.durationMin();
        BigDecimal fare = fareCalculator.calculateNow(distance, duration);
        Ride ride = Ride.requested(UUID.fromString(user.userId()), request.pickupLat(), request.pickupLng(), request.pickupAddress(),
            request.destLat(), request.destLng(), request.destAddress(), BigDecimal.valueOf(distance).setScale(3, java.math.RoundingMode.HALF_UP), duration, fare);
        ride = rides.save(ride);
        history.save(new RideStatusHistory(ride.getId(), null, RideStatus.REQUESTED, UUID.fromString(user.userId()), "Ride requested"));
        return ride;
    }

    public RideDtos.FareEstimateResponse estimate(double pickupLat, double pickupLng, double destLat, double destLng,
                                                    int durationMin) {
        validateCoordinates(pickupLat, pickupLng, destLat, destLng);
        if (durationMin < 1 || durationMin > 600) throw DomainException.badRequest("BAD_REQUEST", "durationMin must be between 1 and 600");
        double distance = fareCalculator.distanceKm(pickupLat, pickupLng, destLat, destLng);
        return new RideDtos.FareEstimateResponse(fareCalculator.calculateNow(distance, durationMin),
            BigDecimal.valueOf(distance).setScale(3, java.math.RoundingMode.HALF_UP), durationMin, "LKR");
    }

    public RideDtos.FareEstimateResponse estimate(UUID id, AuthenticatedUser user) {
        Ride ride = get(id, user);
        return new RideDtos.FareEstimateResponse(ride.getEstimatedFare(), ride.getDistanceKm(), ride.getDurationMin(), "LKR");
    }

    public List<Ride> list(AuthenticatedUser user, UUID passengerId, UUID driverId, RideStatus status) {
        if (user.isAdmin()) {
            if (passengerId != null && status != null) return rides.findByPassengerIdAndStatusOrderByCreatedAtDesc(passengerId, status);
            if (driverId != null && status != null) return rides.findByDriverIdAndStatusOrderByCreatedAtDesc(driverId, status);
            if (passengerId != null) return rides.findByPassengerIdOrderByCreatedAtDesc(passengerId);
            if (driverId != null) return rides.findByDriverIdOrderByCreatedAtDesc(driverId);
            if (status != null) return rides.findByStatusOrderByCreatedAtDesc(status);
            return rides.findAllByOrderByCreatedAtDesc();
        }
        UUID ownId = UUID.fromString(user.userId());
        if (user.isPassenger()) return status == null ? rides.findByPassengerIdOrderByCreatedAtDesc(ownId)
            : rides.findByPassengerIdAndStatusOrderByCreatedAtDesc(ownId, status);
        if (user.isDriver()) return status == null ? rides.findByDriverIdOrderByCreatedAtDesc(ownId)
            : rides.findByDriverIdAndStatusOrderByCreatedAtDesc(ownId, status);
        throw DomainException.forbidden("Ride access is not allowed");
    }

    public Ride get(UUID id, AuthenticatedUser user) {
        Ride ride = find(id); authorizeView(ride, user); return ride;
    }

    @Transactional
    public Ride assign(UUID id, RideRequests.AssignRequest request, AuthenticatedUser user, String bearerToken) {
        Ride ride = lock(id); requireRole(user, "ADMIN", "SYSTEM");
        UUID driverId = request.driverId();
        if (driverId == null) driverId = assignment.select(ride.getPickupLat(), ride.getPickupLng(), bearerToken);
        accounts.validateUser(driverId.toString(), "DRIVER", bearerToken);
        if (ride.getDriverId() != null) throw DomainException.conflict("DRIVER_ALREADY_ASSIGNED", "A driver is already assigned to this ride");
        change(ride, RideStatus.ASSIGNED, UUID.fromString(user.userId()), "Driver assigned");
        ride.setDriverId(driverId);
        return rides.save(ride);
    }

    @Transactional
    public Ride assignNearest(UUID id, AuthenticatedUser user, String bearerToken) {
        Ride ride = lock(id); requireRole(user, "ADMIN", "SYSTEM");
        UUID driverId = assignment.select(ride.getPickupLat(), ride.getPickupLng(), bearerToken);
        accounts.validateUser(driverId.toString(), "DRIVER", bearerToken);
        if (ride.getDriverId() != null) throw DomainException.conflict("DRIVER_ALREADY_ASSIGNED", "A driver is already assigned to this ride");
        change(ride, RideStatus.ASSIGNED, UUID.fromString(user.userId()), "Nearest available driver assigned");
        ride.setDriverId(driverId); return rides.save(ride);
    }

    @Transactional
    public Ride accept(UUID id, AuthenticatedUser user, String bearerToken) {
        Ride ride = lock(id); requireDriver(ride, user); accounts.validateUser(user.userId(), "DRIVER", bearerToken);
        change(ride, RideStatus.ACCEPTED, UUID.fromString(user.userId()), "Driver accepted"); return rides.save(ride);
    }

    @Transactional
    public Ride start(UUID id, AuthenticatedUser user) {
        Ride ride = lock(id); requireDriver(ride, user);
        change(ride, RideStatus.IN_PROGRESS, UUID.fromString(user.userId()), "Ride started"); return rides.save(ride);
    }

    public Ride complete(UUID id, AuthenticatedUser user) {
        Ride ride = completionService.complete(id, user);
        RidePayment payment = paymentService.process(ride);
        events.publish(ride.getId(), ride.getPassengerId(), ride.getDriverId(), ride.getFinalFare(), ride.getCompletedAt());
        if (payment.getStatus().name().equals("FAILED")) {
            throw new DomainException(org.springframework.http.HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", "Simulated payment failed");
        }
        return ride;
    }

    @Transactional
    public Ride cancel(UUID id, RideRequests.CancelRequest request, AuthenticatedUser user) {
        Ride ride = lock(id);
        if (user.isAdmin()) { /* Admin may force-cancel any non-terminal ride. */ }
        else if (user.isPassenger()) {
            if (!ride.getPassengerId().toString().equals(user.userId())) throw DomainException.forbidden("You can only cancel your own rides");
        } else if (user.isDriver()) requireDriver(ride, user);
        else throw DomainException.forbidden("Ride cancellation is not allowed");
        change(ride, RideStatus.CANCELLED, UUID.fromString(user.userId()), request.reason());
        ride.setCancellationReason(request.reason()); return rides.save(ride);
    }

    public RidePayment receipt(UUID id, AuthenticatedUser user) {
        Ride ride = find(id); authorizeView(ride, user);
        if (ride.getStatus() != RideStatus.COMPLETED) throw DomainException.conflict("RIDE_NOT_COMPLETED", "Receipt is available after ride completion");
        return payments.findByRideId(id).orElseThrow(() -> DomainException.notFound("Payment receipt not found"));
    }

    public List<RideStatusHistory> history(UUID id) {
        find(id);
        return history.findByRideIdOrderByChangedAtAsc(id);
    }

    private void change(Ride ride, RideStatus next, UUID actor, String reason) {
        transitions.validate(ride.getStatus(), next);
        RideStatus previous = ride.getStatus(); ride.setStatus(next);
        history.save(new RideStatusHistory(ride.getId(), previous, next, actor, reason));
    }
    private Ride find(UUID id) { return rides.findById(id).orElseThrow(() -> DomainException.notFound("Ride not found")); }
    private Ride lock(UUID id) { return rides.findByIdForUpdate(id).orElseThrow(() -> DomainException.notFound("Ride not found")); }
    private static void requireRole(AuthenticatedUser user, String... roles) {
        for (String role : roles) if (role.equals(user.role())) return;
        throw DomainException.forbidden("This action is not allowed for your role");
    }
    private static void requireDriver(Ride ride, AuthenticatedUser user) {
        if (!user.isDriver()) throw DomainException.forbidden("Only a driver can perform this action");
        if (ride.getDriverId() == null || !ride.getDriverId().toString().equals(user.userId())) throw DomainException.forbidden("Ride is not assigned to this driver");
    }
    private static void authorizeView(Ride ride, AuthenticatedUser user) {
        if (user.isAdmin()) return;
        if (user.isPassenger() && ride.getPassengerId().toString().equals(user.userId())) return;
        if (user.isDriver() && ride.getDriverId() != null && ride.getDriverId().toString().equals(user.userId())) return;
        throw DomainException.forbidden("You cannot view this ride");
    }
    private static void validateCoordinates(double pickupLat, double pickupLng, double destLat, double destLng) {
        if (pickupLat < -90 || pickupLat > 90 || destLat < -90 || destLat > 90
            || pickupLng < -180 || pickupLng > 180 || destLng < -180 || destLng > 180) {
            throw DomainException.badRequest("BAD_REQUEST", "Coordinates are outside the valid latitude/longitude range");
        }
    }
}
