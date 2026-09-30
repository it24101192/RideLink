package com.ridelink.farepayment.repository.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ridelink.farepayment.model.FareEstimate;

public interface SpringDataFareEstimateRepository extends JpaRepository<FareEstimate, UUID> {
    List<FareEstimate> findAllByRideRequestId(UUID rideRequestId);
    List<FareEstimate> findAllByPassengerId(String passengerId);
}
