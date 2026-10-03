package com.ridelink.drivervehicle.repository;

import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import com.ridelink.drivervehicle.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    boolean existsByLicenseNo(String licenseNo);

    boolean existsByLicenseNoAndIdNot(String licenseNo, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Driver> findByAvailability(AvailabilityStatus availability);

    List<Driver> findByAvailabilityAndServiceAreaIgnoreCase(AvailabilityStatus availability, String serviceArea);
}
