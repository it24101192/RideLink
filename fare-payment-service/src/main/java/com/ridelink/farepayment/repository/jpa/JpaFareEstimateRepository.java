package com.ridelink.farepayment.repository.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.ridelink.farepayment.model.FareEstimate;
import com.ridelink.farepayment.repository.port.FareEstimateRepository;

@Repository
@Transactional(readOnly = true)
public class JpaFareEstimateRepository implements FareEstimateRepository {
    private final SpringDataFareEstimateRepository jpaRepository;

    public JpaFareEstimateRepository(SpringDataFareEstimateRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public FareEstimate create(FareEstimate fareEstimate) { return jpaRepository.save(fareEstimate); }

    @Override
    public Optional<FareEstimate> findById(UUID id) { return jpaRepository.findById(id); }

    @Override
    public List<FareEstimate> findByRideRequestId(UUID rideRequestId) {
        return jpaRepository.findAllByRideRequestId(rideRequestId);
    }

    @Override
    public List<FareEstimate> findByPassengerId(String passengerId) {
        return jpaRepository.findAllByPassengerId(passengerId);
    }
}
