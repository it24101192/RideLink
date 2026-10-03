package com.ridelink.farepayment.messaging;

import java.math.BigDecimal;

public record RideCompletedEvent(String rideId, double actualDistanceKm,
                                 int actualDurationMinutes, BigDecimal surgeMultiplier) { }
