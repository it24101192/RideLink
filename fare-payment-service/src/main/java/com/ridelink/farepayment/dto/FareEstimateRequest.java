package com.ridelink.farepayment.dto;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FareEstimateRequest(UUID rideRequestId, String passengerId,
                                 @NotBlank @Size(max = 500) String pickupLocation,
                                 @NotBlank @Size(max = 500) String destinationLocation,
                                 @DecimalMin(value = "0.0", inclusive = false) double distanceKm,
                                 @Min(0) int durationMinutes,
                                 @DecimalMin("1.0") @DecimalMax("2.5") BigDecimal surgeMultiplier) {
    public FareEstimateRequest withPassengerId(String authenticatedPassengerId) {
        return new FareEstimateRequest(rideRequestId, authenticatedPassengerId, pickupLocation,
            destinationLocation, distanceKm, durationMinutes, surgeMultiplier);
    }
}
