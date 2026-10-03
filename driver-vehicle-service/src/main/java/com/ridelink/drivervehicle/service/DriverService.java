package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.dto.request.DriverRequestDto;
import com.ridelink.drivervehicle.dto.response.AvailableDriverResponseDto;
import com.ridelink.drivervehicle.dto.response.DriverResponseDto;
import com.ridelink.drivervehicle.dto.response.EligibleDriverResponseDto;
import com.ridelink.drivervehicle.entity.AvailabilityStatus;

import java.util.List;

public interface DriverService {

    DriverResponseDto registerDriver(DriverRequestDto request);

    List<DriverResponseDto> getAllDrivers();

    DriverResponseDto getDriverById(Long id);

    DriverResponseDto updateDriver(Long id, DriverRequestDto request);

    void deleteDriver(Long id);

    DriverResponseDto updateAvailability(Long id, AvailabilityStatus availability);

    DriverResponseDto updateLocation(Long id, Double latitude, Double longitude);

    List<DriverResponseDto> getAvailableDrivers();

    List<EligibleDriverResponseDto> getEligibleAvailableDrivers(String serviceArea);

    /**
     * AVAILABLE drivers that have a linked account and a known location within
     * radiusKm of (lat, lng), nearest first. Used by Ride Management Service.
     */
    List<AvailableDriverResponseDto> getNearbyAvailableDrivers(double lat, double lng, double radiusKm);
}
