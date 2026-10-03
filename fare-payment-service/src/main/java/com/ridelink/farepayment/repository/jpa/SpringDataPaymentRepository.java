package com.ridelink.farepayment.repository.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentStatus;

public interface SpringDataPaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findAllByRideId(UUID rideId);
    List<Payment> findAllByPassengerId(String passengerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Payment p set p.status = :status, p.failureReason = :failureReason, p.refundReason = :refundReason, p.updatedAt = CURRENT_TIMESTAMP where p.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") PaymentStatus status,
                     @Param("failureReason") String failureReason,
                     @Param("refundReason") String refundReason);
}
