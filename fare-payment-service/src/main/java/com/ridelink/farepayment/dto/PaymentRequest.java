package com.ridelink.farepayment.dto;

import java.math.BigDecimal;
import java.util.UUID;
import com.ridelink.farepayment.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentRequest(@NotBlank String rideId, UUID fareEstimateId,
                             @NotBlank @Size(max = 100) String passengerId,
                             @NotBlank @Size(max = 100) String driverId,
                             @Positive double actualDistanceKm,
                             @Min(0) int actualDurationMinutes,
                             @DecimalMin("1.0") @DecimalMax("2.5") BigDecimal surgeMultiplier,
                             @NotNull PaymentMethod paymentMethod,
                             @NotBlank @Size(max = 100) String transactionRef) { }
