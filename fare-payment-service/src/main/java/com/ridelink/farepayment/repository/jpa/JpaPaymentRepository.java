package com.ridelink.farepayment.repository.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentStatus;
import com.ridelink.farepayment.repository.port.PaymentRepository;

@Repository
@Transactional(readOnly = true)
public class JpaPaymentRepository implements PaymentRepository {
    private final SpringDataPaymentRepository jpaRepository;

    public JpaPaymentRepository(SpringDataPaymentRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Payment create(Payment payment) { return jpaRepository.save(payment); }

    @Override
    public Optional<Payment> findById(UUID id) { return jpaRepository.findById(id); }

    @Override
    public List<Payment> findByRideId(UUID rideId) { return jpaRepository.findAllByRideId(rideId); }

    @Override
    public List<Payment> findByPassengerId(String passengerId) {
        return jpaRepository.findAllByPassengerId(passengerId);
    }

    @Override
    @Transactional
    public boolean updateStatus(UUID id, PaymentStatus status, String failureReason, String refundReason) {
        return jpaRepository.updateStatus(id, status, failureReason, refundReason) == 1;
    }
}
