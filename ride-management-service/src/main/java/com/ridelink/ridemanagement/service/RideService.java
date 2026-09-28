package com.ridelink.ridemanagement.service;

import com.ridelink.ridemanagement.client.DriverServiceClient;
import com.ridelink.ridemanagement.client.FarePaymentServiceClient;
import com.ridelink.ridemanagement.dto.AssignDriverRequest;
import com.ridelink.ridemanagement.dto.CreateRideRequest;
import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.entity.RideStatusHistory;
import com.ridelink.ridemanagement.enums.RideStatus;
import com.ridelink.ridemanagement.exception.InvalidStatusTransitionException;
import com.ridelink.ridemanagement.exception.NoDriverAvailableException;
import com.ridelink.ridemanagement.exception.RideNotFoundException;
import com.ridelink.ridemanagement.repository.RideRepository;
import com.ridelink.ridemanagement.repository.RideStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final RideStatusHistoryRepository historyRepository;
    private final DriverServiceClient driverServiceClient;
    private final FarePaymentServiceClient farePaymentServiceClient;

    // Documented, explicit state-transition table — the single source of
    // truth for which status changes are legal. Keeping it in one place
    // makes the lifecycle rule easy to test and to defend in the viva.
    private static final Map<RideStatus, EnumSet<RideStatus>> VALID_TRANSITIONS = new EnumMap<>(RideStatus.class);

    static {
        VALID_TRANSITIONS.put(RideStatus.REQUESTED, EnumSet.of(RideStatus.ASSIGNED, RideStatus.CANCELLED));
        VALID_TRANSITIONS.put(RideStatus.ASSIGNED, EnumSet.of(RideStatus.ACCEPTED, RideStatus.CANCELLED));
        VALID_TRANSITIONS.put(RideStatus.ACCEPTED, EnumSet.of(RideStatus.IN_PROGRESS, RideStatus.CANCELLED));
        VALID_TRANSITIONS.put(RideStatus.IN_PROGRESS, EnumSet.of(RideStatus.COMPLETED));
        VALID_TRANSITIONS.put(RideStatus.COMPLETED, EnumSet.noneOf(RideStatus.class));
        VALID_TRANSITIONS.put(RideStatus.CANCELLED, EnumSet.noneOf(RideStatus.class));
    }

    public RideService(RideRepository rideRepository,
                        RideStatusHistoryRepository historyRepository,
                        DriverServiceClient driverServiceClient,
                        FarePaymentServiceClient farePaymentServiceClient) {
        this.rideRepository = rideRepository;
        this.historyRepository = historyRepository;
        this.driverServiceClient = driverServiceClient;
        this.farePaymentServiceClient = farePaymentServiceClient;
    }

    @Transactional
    public Ride createRide(CreateRideRequest request) {
        Double estimatedFare;
        try {
            estimatedFare = farePaymentServiceClient.getFareEstimate(
                    request.getPickupLocation(), request.getDestination());
        } catch (Exception ex) {
            // Fare estimate is a nice-to-have at creation time; don't block
            // ride creation if the Fare & Payment Service is unreachable.
            estimatedFare = null;
        }

        Ride ride = Ride.builder()
                .passengerId(request.getPassengerId())
                .pickupLocation(request.getPickupLocation())
                .destination(request.getDestination())
                .status(RideStatus.REQUESTED)
                .estimatedFare(estimatedFare)
                .requestedAt(LocalDateTime.now())
                .build();

        Ride saved = rideRepository.save(ride);
        recordHistory(saved.getId(), null, RideStatus.REQUESTED);
        return saved;
    }

    public Ride getRide(Long id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new RideNotFoundException(id));
    }

    public List<Ride> getRidesByPassenger(Long passengerId) {
        return rideRepository.findByPassengerId(passengerId);
    }

    public List<Ride> getRidesByDriver(Long driverId) {
        return rideRepository.findByDriverId(driverId);
    }

    @Transactional
    public Ride assignDriver(Long rideId, AssignDriverRequest request) {
        Ride ride = getRide(rideId);
        transitionTo(ride, RideStatus.ASSIGNED);

        // Confirm at least one eligible driver currently exists before
        // committing the assignment (real-time check against Driver & Vehicle Service).
        var availableDrivers = driverServiceClient.getAvailableDrivers(ride.getPickupLocation());
        if (availableDrivers == null || availableDrivers.isEmpty()) {
            throw new NoDriverAvailableException(
                    "No available drivers found near " + ride.getPickupLocation());
        }

        ride.setDriverId(request.getDriverId());
        ride.setAssignedAt(LocalDateTime.now());
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride acceptRide(Long rideId) {
        Ride ride = getRide(rideId);
        transitionTo(ride, RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride startRide(Long rideId) {
        Ride ride = getRide(rideId);
        transitionTo(ride, RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now());
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride completeRide(Long rideId) {
        Ride ride = getRide(rideId);
        transitionTo(ride, RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        Ride saved = rideRepository.save(ride);

        // Trigger the Fare & Payment Service once the ride is confirmed complete.
        try {
            farePaymentServiceClient.triggerFinalFareCalculation(
                    saved.getId(), saved.getPassengerId(), saved.getDriverId());
        } catch (Exception ex) {
            // Ride completion itself must not fail because of a downstream
            // payment-service hiccup; log and let a retry/reconciliation job handle it.
        }
        return saved;
    }

    @Transactional
    public Ride cancelRide(Long rideId, String reason) {
        Ride ride = getRide(rideId);
        transitionTo(ride, RideStatus.CANCELLED);
        ride.setCancelledAt(LocalDateTime.now());
        ride.setCancellationReason(reason);
        return rideRepository.save(ride);
    }

    // --- Internal helpers -------------------------------------------------

    private void transitionTo(Ride ride, RideStatus newStatus) {
        RideStatus current = ride.getStatus();
        EnumSet<RideStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(RideStatus.class));

        if (!allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition ride " + ride.getId() + " from " + current + " to " + newStatus);
        }
        ride.setStatus(newStatus);
        recordHistory(ride.getId(), current, newStatus);
    }

    private void recordHistory(Long rideId, RideStatus previous, RideStatus newStatus) {
        historyRepository.save(RideStatusHistory.builder()
                .rideId(rideId)
                .previousStatus(previous != null ? previous.name() : null)
                .newStatus(newStatus.name())
                .changedAt(LocalDateTime.now())
                .build());
    }
}
