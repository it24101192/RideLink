package com.ridelink.ridemanagement.repository;

import com.ridelink.ridemanagement.model.Ride;
import com.ridelink.ridemanagement.model.RideStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") UUID id);
    List<Ride> findByPassengerIdOrderByCreatedAtDesc(UUID passengerId);
    List<Ride> findByDriverIdOrderByCreatedAtDesc(UUID driverId);
    List<Ride> findByStatusOrderByCreatedAtDesc(RideStatus status);
    List<Ride> findAllByOrderByCreatedAtDesc();
    List<Ride> findByPassengerIdAndStatusOrderByCreatedAtDesc(UUID passengerId, RideStatus status);
    List<Ride> findByDriverIdAndStatusOrderByCreatedAtDesc(UUID driverId, RideStatus status);
}
