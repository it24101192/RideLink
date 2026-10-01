package com.ridelink.ridemanagement.repository;

import com.ridelink.ridemanagement.model.RideStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideStatusHistoryRepository extends JpaRepository<RideStatusHistory, UUID> {
    List<RideStatusHistory> findByRideIdOrderByChangedAtAsc(UUID rideId);
}
