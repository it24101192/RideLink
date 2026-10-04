package com.ridelink.ridemanagement.repository;

import com.ridelink.ridemanagement.model.RideOutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideOutboxEventRepository extends JpaRepository<RideOutboxEvent, UUID> {
    List<RideOutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
