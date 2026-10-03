package com.ridelink.ridemanagement.repository;

import com.ridelink.ridemanagement.model.RidePayment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RidePaymentRepository extends JpaRepository<RidePayment, UUID> {
    Optional<RidePayment> findByRideId(UUID rideId);
}
