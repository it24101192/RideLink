package com.ridelink.farepayment.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.ridelink.farepayment.model.PaymentMethod;
import com.ridelink.farepayment.model.PaymentStatus;

public record ReceiptDto(UUID paymentId, UUID rideId, String passengerId, String driverId,
                         int amount, String currency, PaymentStatus status,
                         PaymentMethod paymentMethod, String transactionRef,
                         LocalDateTime paidAt) { }
