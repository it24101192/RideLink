package com.ridelink.farepayment.repository.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.ridelink.farepayment.model.FareEstimate;

public interface FareEstimateRepository {
    FareEstimate create(FareEstimate fareEstimate);
    Optional<FareEstimate> findById(UUID id);
    List<FareEstimate> findByRideRequestId(UUID rideRequestId);
    List<FareEstimate> findByPassengerId(String passengerId);
}
