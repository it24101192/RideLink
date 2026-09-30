package com.ridelink.farepayment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record FareEstimateDto(UUID id, UUID rideRequestId, String passengerId,
                              String pickupLocation, String destinationLocation,
                              BigDecimal distanceKm, int durationMinutes,
                              BigDecimal surgeMultiplier, int estimatedFare,
                              Map<String, Object> breakdown, LocalDateTime createdAt,
                              String currency) { }
