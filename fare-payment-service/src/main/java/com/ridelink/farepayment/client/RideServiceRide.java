package com.ridelink.farepayment.client;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAlias;

/** Minimal typed view of Ride Management's ride resource. */
public record RideServiceRide(@JsonAlias("rideId") UUID id, String status) {
    public boolean isCompleted() { return "COMPLETED".equalsIgnoreCase(status); }
}
