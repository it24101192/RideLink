package com.ridelink.farepayment.messaging;

import java.time.Instant;
import java.util.UUID;
import com.ridelink.farepayment.model.PaymentStatus;

public record PaymentCompletedEvent(UUID paymentId, UUID rideId, String passengerId,
                                    String driverId, int amount, String currency,
                                    PaymentStatus status, Instant timestamp) { }
