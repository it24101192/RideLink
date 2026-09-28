package com.ridelink.ridemanagement.repository;

import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByPassengerId(Long passengerId);

    List<Ride> findByDriverId(Long driverId);

    List<Ride> findByStatus(RideStatus status);
}
