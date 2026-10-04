package com.ridelink.drivervehicle.service.impl;

import com.ridelink.drivervehicle.dto.request.DriverRequestDto;
import com.ridelink.drivervehicle.dto.response.AvailableDriverResponseDto;
import com.ridelink.drivervehicle.dto.response.DriverResponseDto;
import com.ridelink.drivervehicle.dto.response.EligibleDriverResponseDto;
import com.ridelink.drivervehicle.dto.response.VehicleResponseDto;
import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.exception.DuplicateResourceException;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import com.ridelink.drivervehicle.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public DriverResponseDto registerDriver(DriverRequestDto request) {
        log.info("Registering new driver with license: {}", request.getLicenseNo());

        if (driverRepository.existsByLicenseNo(request.getLicenseNo().trim())) {
            throw new DuplicateResourceException(
                    "Driver with license number '" + request.getLicenseNo() + "' already exists");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()
                && driverRepository.existsByEmail(request.getEmail().trim())) {
            throw new DuplicateResourceException(
                    "Driver with email '" + request.getEmail() + "' already exists");
        }

        Driver driver = Driver.builder()
                .accountId(request.getAccountId())
                .name(request.getName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null)
                .licenseNo(request.getLicenseNo().trim())
                .availability(request.getAvailability() != null ? request.getAvailability() : AvailabilityStatus.AVAILABLE)
                .serviceArea(request.getServiceArea() != null && !request.getServiceArea().trim().isEmpty() ? request.getServiceArea().trim() : null)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationUpdatedAt(request.getLatitude() != null || request.getLongitude() != null ? LocalDateTime.now() : null)
                .build();

        Driver savedDriver = driverRepository.save(driver);
        log.info("Driver registered successfully with ID: {}", savedDriver.getId());
        return mapToResponseDto(savedDriver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponseDto> getAllDrivers() {
        log.debug("Fetching all drivers");
        return driverRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponseDto getDriverById(Long id) {
        log.debug("Fetching driver with ID: {}", id);
        Driver driver = findDriverOrThrow(id);
        return mapToResponseDto(driver);
    }

    @Override
    public DriverResponseDto updateDriver(Long id, DriverRequestDto request) {
        log.info("Updating driver profile with ID: {}", id);
        Driver driver = findDriverOrThrow(id);

        if (driverRepository.existsByLicenseNoAndIdNot(request.getLicenseNo().trim(), id)) {
            throw new DuplicateResourceException(
                    "License number '" + request.getLicenseNo() + "' is already in use by another driver");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()
                && driverRepository.existsByEmailAndIdNot(request.getEmail().trim(), id)) {
            throw new DuplicateResourceException(
                    "Email '" + request.getEmail() + "' is already in use by another driver");
        }

        driver.setAccountId(request.getAccountId());
        driver.setName(request.getName().trim());
        driver.setPhone(request.getPhone().trim());
        driver.setEmail(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null);
        driver.setLicenseNo(request.getLicenseNo().trim());
        if (request.getAvailability() != null) {
            driver.setAvailability(request.getAvailability());
        }
        if (request.getServiceArea() != null) {
            driver.setServiceArea(request.getServiceArea().trim());
        }
        if (request.getLatitude() != null || request.getLongitude() != null) {
            driver.setLatitude(request.getLatitude());
            driver.setLongitude(request.getLongitude());
            driver.setLocationUpdatedAt(LocalDateTime.now());
        }

        Driver updatedDriver = driverRepository.save(driver);
        log.info("Driver profile updated for ID: {}", id);
        return mapToResponseDto(updatedDriver);
    }

    @Override
    public void deleteDriver(Long id) {
        log.info("Deleting driver with ID: {}", id);
        Driver driver = findDriverOrThrow(id);
        vehicleRepository.deleteByDriverId(id);
        driverRepository.delete(driver);
        log.info("Driver with ID: {} and their vehicles deleted successfully", id);
    }

    @Override
    public DriverResponseDto updateAvailability(Long id, AvailabilityStatus availability) {
        log.info("Updating availability for driver ID: {} to {}", id, availability);
        Driver driver = findDriverOrThrow(id);
        driver.setAvailability(availability);
        Driver updatedDriver = driverRepository.save(driver);
        return mapToResponseDto(updatedDriver);
    }

    @Override
    public DriverResponseDto updateLocation(Long id, Double latitude, Double longitude) {
        log.info("Updating simulated location for driver ID: {} to ({}, {})", id, latitude, longitude);
        Driver driver = findDriverOrThrow(id);
        driver.setLatitude(latitude);
        driver.setLongitude(longitude);
        driver.setLocationUpdatedAt(LocalDateTime.now());
        Driver updatedDriver = driverRepository.save(driver);
        return mapToResponseDto(updatedDriver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponseDto> getAvailableDrivers() {
        log.debug("Fetching all available drivers");
        return driverRepository.findByAvailability(AvailabilityStatus.AVAILABLE).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibleDriverResponseDto> getEligibleAvailableDrivers(String serviceArea) {
        log.info("Searching for eligible available drivers with serviceArea: {}", serviceArea);
        List<Driver> availableDrivers;
        if (serviceArea != null && !serviceArea.trim().isEmpty()) {
            availableDrivers = driverRepository.findByAvailabilityAndServiceAreaIgnoreCase(
                    AvailabilityStatus.AVAILABLE,
                    serviceArea.trim()
            );
        } else {
            availableDrivers = driverRepository.findByAvailability(AvailabilityStatus.AVAILABLE);
        }

        List<EligibleDriverResponseDto> eligibleDrivers = new ArrayList<>();
        for (Driver driver : availableDrivers) {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findFirstByDriverId(driver.getId());
            if (vehicleOpt.isPresent()) {
                Vehicle vehicle = vehicleOpt.get();
                EligibleDriverResponseDto dto = EligibleDriverResponseDto.builder()
                        .driverId(driver.getId())
                        .accountId(driver.getAccountId())
                        .driverName(driver.getName())
                        .vehicleId(vehicle.getId())
                        .vehicleNumber(vehicle.getPlateNumber())
                        .vehicleType(vehicle.getVehicleType())
                        .availability(driver.getAvailability())
                        .serviceArea(driver.getServiceArea())
                        .latitude(driver.getLatitude())
                        .longitude(driver.getLongitude())
                        .locationUpdatedAt(driver.getLocationUpdatedAt())
                        .build();
                eligibleDrivers.add(dto);
            }
        }
        return eligibleDrivers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableDriverResponseDto> getNearbyAvailableDrivers(double lat, double lng, double radiusKm) {
        log.info("Searching for available drivers within {} km of ({}, {})", radiusKm, lat, lng);
        return driverRepository.findByAvailability(AvailabilityStatus.AVAILABLE).stream()
                .filter(d -> d.getAccountId() != null && d.getLatitude() != null && d.getLongitude() != null)
                .map(d -> AvailableDriverResponseDto.builder()
                        .id(String.valueOf(d.getAccountId()))
                        .driverId(d.getId())
                        .lat(d.getLatitude())
                        .lng(d.getLongitude())
                        .serviceArea(d.getServiceArea())
                        .distanceKm(haversineKm(lat, lng, d.getLatitude(), d.getLongitude()))
                        .build())
                .filter(dto -> dto.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(AvailableDriverResponseDto::getDistanceKm))
                .collect(Collectors.toList());
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Driver findDriverOrThrow(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }

    private DriverResponseDto mapToResponseDto(Driver driver) {
        List<VehicleResponseDto> vehicles = vehicleRepository.findByDriverId(driver.getId()).stream()
                .map(v -> VehicleResponseDto.builder()
                        .id(v.getId())
                        .driverId(v.getDriverId())
                        .vehicleType(v.getVehicleType())
                        .model(v.getModel())
                        .plateNumber(v.getPlateNumber())
                        .build())
                .collect(Collectors.toList());

        return DriverResponseDto.builder()
                .id(driver.getId())
                .accountId(driver.getAccountId())
                .name(driver.getName())
                .phone(driver.getPhone())
                .email(driver.getEmail())
                .licenseNo(driver.getLicenseNo())
                .availability(driver.getAvailability())
                .serviceArea(driver.getServiceArea())
                .latitude(driver.getLatitude())
                .longitude(driver.getLongitude())
                .locationUpdatedAt(driver.getLocationUpdatedAt())
                .vehicles(vehicles)
                .build();
    }
}
