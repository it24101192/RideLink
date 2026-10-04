package com.ridelink.farepayment.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/** Must remain compatible with Ride Management's versioned event contract. */
public record RideCompletedEvent(String eventId, String eventType, String eventVersion,
        Instant occurredAt, String rideId, String passengerId, String driverId,
        BigDecimal distanceKm, int durationMinutes, BigDecimal fareAmount, String currency) { }
