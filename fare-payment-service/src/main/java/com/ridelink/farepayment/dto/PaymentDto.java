package com.ridelink.farepayment.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.ridelink.farepayment.model.PaymentMethod;
import com.ridelink.farepayment.model.PaymentStatus;

public record PaymentDto(UUID id, UUID rideId, UUID fareEstimateId, String passengerId,
                         String driverId, int amount, String currency,
                         PaymentStatus status, PaymentMethod paymentMethod,
                         String transactionRef, String failureReason,
                         LocalDateTime createdAt, LocalDateTime updatedAt) { }
