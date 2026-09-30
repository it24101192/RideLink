package com.ridelink.farepayment.repository.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentStatus;

public interface PaymentRepository {
    Payment create(Payment payment);
    Optional<Payment> findById(UUID id);
    List<Payment> findByRideId(UUID rideId);
    List<Payment> findByPassengerId(String passengerId);
    boolean updateStatus(UUID id, PaymentStatus status, String failureReason, String refundReason);
}
