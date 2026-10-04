package com.ridelink.ridemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ridelink.ridemanagement.client.DriverServiceClient;
import com.ridelink.ridemanagement.error.DomainException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DriverAssignmentServiceTest {
    private final DriverServiceClient clients = mock(DriverServiceClient.class);
    private final DriverAssignmentService assignment = new DriverAssignmentService(clients, 10, true);
    private static final String TOKEN = "Bearer token";
    private static final double LAT = 6.9271;
    private static final double LNG = 79.8612;

    @Test
    void assignmentOnlyConsidersDriversServingTheRequestedArea() {
        String requestedDriverId = "00000000-0000-0000-0000-000000000101";
        when(clients.available(LAT, LNG, 10, TOKEN)).thenReturn(List.of(
                new DriverServiceClient.AvailableDriver(
                        "00000000-0000-0000-0000-000000000102", 6.9272, 79.8612, "Kandy"),
                new DriverServiceClient.AvailableDriver(requestedDriverId, 6.93, 79.87, " colombo ")));

        assertEquals(UUID.fromString(requestedDriverId),
                assignment.select(LAT, LNG, "Colombo", TOKEN));
    }

    @Test
    void enabledServiceAreaCheckRejectsMissingRideArea() {
        when(clients.available(LAT, LNG, 10, TOKEN)).thenReturn(List.of(
                new DriverServiceClient.AvailableDriver(
                        "00000000-0000-0000-0000-000000000101", LAT, LNG, "Colombo")));

        assertThrows(DomainException.class, () -> assignment.select(LAT, LNG, null, TOKEN));
    }
}
