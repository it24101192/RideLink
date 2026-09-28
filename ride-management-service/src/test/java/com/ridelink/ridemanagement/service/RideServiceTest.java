package com.ridelink.ridemanagement.service;

import com.ridelink.ridemanagement.client.DriverServiceClient;
import com.ridelink.ridemanagement.client.FarePaymentServiceClient;
import com.ridelink.ridemanagement.dto.AssignDriverRequest;
import com.ridelink.ridemanagement.dto.AvailableDriverResponse;
import com.ridelink.ridemanagement.dto.CreateRideRequest;
import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.enums.RideStatus;
import com.ridelink.ridemanagement.exception.InvalidStatusTransitionException;
import com.ridelink.ridemanagement.exception.NoDriverAvailableException;
import com.ridelink.ridemanagement.repository.RideRepository;
import com.ridelink.ridemanagement.repository.RideStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private RideStatusHistoryRepository historyRepository;
    @Mock private DriverServiceClient driverServiceClient;
    @Mock private FarePaymentServiceClient farePaymentServiceClient;

    private RideService rideService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rideService = new RideService(rideRepository, historyRepository, driverServiceClient, farePaymentServiceClient);
    }

    @Test
    void createRide_savesRideWithRequestedStatus() {
        CreateRideRequest request = new CreateRideRequest();
        request.setPassengerId(1L);
        request.setPickupLocation("Colombo 03");
        request.setDestination("Airport");

        when(farePaymentServiceClient.getFareEstimate(anyString(), anyString())).thenReturn(3500.0);
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        Ride result = rideService.createRide(request);

        assertEquals(RideStatus.REQUESTED, result.getStatus());
        assertEquals(3500.0, result.getEstimatedFare());
        verify(rideRepository, times(1)).save(any(Ride.class));
    }

    @Test
    void assignDriver_throwsWhenNoDriversAvailable() {
        Ride ride = Ride.builder()
                .id(1L).passengerId(1L).pickupLocation("Colombo 03")
                .destination("Airport").status(RideStatus.REQUESTED)
                .requestedAt(LocalDateTime.now()).build();

        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers(anyString())).thenReturn(List.of());

        AssignDriverRequest request = new AssignDriverRequest();
        request.setDriverId(50L);

        assertThrows(NoDriverAvailableException.class, () -> rideService.assignDriver(1L, request));
    }

    @Test
    void assignDriver_succeedsWhenDriverAvailable() {
        Ride ride = Ride.builder()
                .id(1L).passengerId(1L).pickupLocation("Colombo 03")
                .destination("Airport").status(RideStatus.REQUESTED)
                .requestedAt(LocalDateTime.now()).build();

        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(driverServiceClient.getAvailableDrivers(anyString()))
                .thenReturn(List.of(new AvailableDriverResponse(50L, "Kamal", "CAB-1234", "Colombo 03")));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignDriverRequest request = new AssignDriverRequest();
        request.setDriverId(50L);

        Ride result = rideService.assignDriver(1L, request);

        assertEquals(RideStatus.ASSIGNED, result.getStatus());
        assertEquals(50L, result.getDriverId());
    }

    @Test
    void completeRide_throwsWhenRideNotInProgress() {
        // Negative scenario: invalid transition e.g. REQUESTED -> COMPLETED directly
        Ride ride = Ride.builder()
                .id(2L).passengerId(1L).pickupLocation("A")
                .destination("B").status(RideStatus.REQUESTED)
                .requestedAt(LocalDateTime.now()).build();

        when(rideRepository.findById(2L)).thenReturn(Optional.of(ride));

        assertThrows(InvalidStatusTransitionException.class, () -> rideService.completeRide(2L));
    }

    @Test
    void cancelRide_allowedFromRequestedStatus() {
        Ride ride = Ride.builder()
                .id(3L).passengerId(1L).pickupLocation("A")
                .destination("B").status(RideStatus.REQUESTED)
                .requestedAt(LocalDateTime.now()).build();

        when(rideRepository.findById(3L)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        Ride result = rideService.cancelRide(3L, "Passenger changed mind");

        assertEquals(RideStatus.CANCELLED, result.getStatus());
        assertEquals("Passenger changed mind", result.getCancellationReason());
    }
}
