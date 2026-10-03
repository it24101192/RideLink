package com.ridelink.drivervehicle.repository;

import com.ridelink.drivervehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlateNumber(String plateNumber);

    boolean existsByPlateNumberAndIdNot(String plateNumber, Long id);

    List<Vehicle> findByDriverId(Long driverId);

    Optional<Vehicle> findFirstByDriverId(Long driverId);

    void deleteByDriverId(Long driverId);
}
