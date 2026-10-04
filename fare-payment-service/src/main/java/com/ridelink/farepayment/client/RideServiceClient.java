package com.ridelink.farepayment.client;

import java.util.Optional;
import java.util.UUID;

/** Port for the Ride Management REST contract. */
public interface RideServiceClient {
    Optional<RideServiceRide> getRide(UUID rideId, String bearerToken);
}
