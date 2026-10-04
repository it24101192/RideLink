package com.ridelink.ridemanagement.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/** Versioned event contract shared with Fare & Payment Service. */
public record RideCompletedEvent(String eventId, String eventType, String eventVersion,
        Instant occurredAt, String rideId, String passengerId, String driverId,
        BigDecimal distanceKm, int durationMinutes, BigDecimal fareAmount, String currency) { }
