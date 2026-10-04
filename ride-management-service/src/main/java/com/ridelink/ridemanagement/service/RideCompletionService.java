package com.ridelink.ridemanagement.service;

import com.ridelink.ridemanagement.domain.FareCalculator;
import com.ridelink.ridemanagement.domain.StatusTransitionValidator;
import com.ridelink.ridemanagement.error.DomainException;
import com.ridelink.ridemanagement.model.Ride;
import com.ridelink.ridemanagement.model.RideStatus;
import com.ridelink.ridemanagement.model.RideStatusHistory;
import com.ridelink.ridemanagement.repository.RideRepository;
import com.ridelink.ridemanagement.repository.RideStatusHistoryRepository;
import com.ridelink.ridemanagement.security.AuthenticatedUser;
import com.ridelink.ridemanagement.messaging.RideCompletedEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideCompletionService {
    private final RideRepository rides;
    private final RideStatusHistoryRepository history;
    private final FareCalculator fares;
    private final StatusTransitionValidator transitions;
    private final RideCompletedEventPublisher events;
    public RideCompletionService(RideRepository rides, RideStatusHistoryRepository history,
                                 FareCalculator fares, StatusTransitionValidator transitions,
                                 RideCompletedEventPublisher events) {
        this.rides = rides; this.history = history; this.fares = fares;
        this.transitions = transitions; this.events = events;
    }
    @Transactional
    public Ride complete(UUID id, AuthenticatedUser user) {
        Ride ride = rides.findByIdForUpdate(id).orElseThrow(() -> DomainException.notFound("Ride not found"));
        if (!user.isDriver()) throw DomainException.forbidden("Only a driver can perform this action");
        if (ride.getDriverId() == null || !ride.getDriverId().toString().equals(user.userId())) {
            throw DomainException.forbidden("Ride is not assigned to this driver");
        }
        transitions.validate(ride.getStatus(), RideStatus.COMPLETED);
        RideStatus previous = ride.getStatus();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());
        ride.setFinalFare(fares.calculateNow(ride.getDistanceKm().doubleValue(), ride.getDurationMin()));
        history.save(new RideStatusHistory(ride.getId(), previous, RideStatus.COMPLETED,
            UUID.fromString(user.userId()), "Ride completed"));
        Ride saved = rides.save(ride);
        events.enqueue(saved);
        return saved;
    }
}
