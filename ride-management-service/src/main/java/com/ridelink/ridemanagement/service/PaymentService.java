package com.ridelink.ridemanagement.service;

import com.ridelink.ridemanagement.error.DomainException;
import com.ridelink.ridemanagement.model.PaymentStatus;
import com.ridelink.ridemanagement.model.Ride;
import com.ridelink.ridemanagement.model.RidePayment;
import com.ridelink.ridemanagement.repository.RidePaymentRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final RidePaymentRepository payments;
    private final boolean testMode;
    private final boolean testOutcomeSuccess;
    public PaymentService(RidePaymentRepository payments, @Value("${payment.test-mode:false}") boolean testMode,
                          @Value("${payment.test-outcome-success:true}") boolean testOutcomeSuccess) {
        this.payments = payments; this.testMode = testMode; this.testOutcomeSuccess = testOutcomeSuccess;
    }
    @Transactional
    public RidePayment process(Ride ride) {
        var existing = payments.findByRideId(ride.getId());
        if (existing.isPresent()) return existing.get();
        boolean paid = testMode ? testOutcomeSuccess : true;
        RidePayment payment = payments.save(new RidePayment(ride.getId(), ride.getFinalFare(),
            paid ? PaymentStatus.PAID : PaymentStatus.FAILED, "SIM-" + UUID.randomUUID()));
        return payment;
    }
}
