package com.ridelink.drivervehicle.service.impl;

import com.ridelink.drivervehicle.dto.request.VehicleRequestDto;
import com.ridelink.drivervehicle.dto.response.VehicleResponseDto;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.exception.DuplicateResourceException;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import com.ridelink.drivervehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    @Override
    public VehicleResponseDto createVehicle(VehicleRequestDto request) {
        log.info("Creating new vehicle with plate number: {}", request.getPlateNumber());

        if (vehicleRepository.existsByPlateNumber(request.getPlateNumber().trim())) {
            throw new DuplicateResourceException(
                    "Vehicle with plate number '" + request.getPlateNumber() + "' already exists");
        }

        if (request.getDriverId() != null && !driverRepository.existsById(request.getDriverId())) {
            throw new ResourceNotFoundException(
                    "Cannot assign vehicle: Driver not found with id: " + request.getDriverId());
        }

        Vehicle vehicle = Vehicle.builder()
                .driverId(request.getDriverId())
                .vehicleType(request.getVehicleType().trim())
                .model(request.getModel().trim())
                .plateNumber(request.getPlateNumber().trim())
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle created successfully with ID: {}", savedVehicle.getId());
        return mapToResponseDto(savedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getAllVehicles() {
        log.debug("Fetching all vehicles");
        return vehicleRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDto getVehicleById(Long id) {
        log.debug("Fetching vehicle with ID: {}", id);
        Vehicle vehicle = findVehicleOrThrow(id);
        return mapToResponseDto(vehicle);
    }

    @Override
    public VehicleResponseDto updateVehicle(Long id, VehicleRequestDto request) {
        log.info("Updating vehicle with ID: {}", id);
        Vehicle vehicle = findVehicleOrThrow(id);

        if (vehicleRepository.existsByPlateNumberAndIdNot(request.getPlateNumber().trim(), id)) {
            throw new DuplicateResourceException(
                    "Plate number '" + request.getPlateNumber() + "' is already in use by another vehicle");
        }

        if (request.getDriverId() != null && !driverRepository.existsById(request.getDriverId())) {
            throw new ResourceNotFoundException(
                    "Cannot assign vehicle: Driver not found with id: " + request.getDriverId());
        }

        vehicle.setDriverId(request.getDriverId());
        vehicle.setVehicleType(request.getVehicleType().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setPlateNumber(request.getPlateNumber().trim());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle updated successfully with ID: {}", id);
        return mapToResponseDto(updatedVehicle);
    }

    @Override
    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle with ID: {}", id);
        Vehicle vehicle = findVehicleOrThrow(id);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle with ID: {} deleted successfully", id);
    }

    @Override
    public VehicleResponseDto assignVehicleToDriver(Long vehicleId, Long driverId) {
        log.info("Assigning vehicle ID: {} to driver ID: {}", vehicleId, driverId);
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Driver not found with id: " + driverId);
        }

        vehicle.setDriverId(driverId);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle ID: {} successfully assigned to driver ID: {}", vehicleId, driverId);
        return mapToResponseDto(updatedVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getVehiclesByDriverId(Long driverId) {
        log.debug("Fetching vehicles for driver ID: {}", driverId);
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Driver not found with id: " + driverId);
        }
        return vehicleRepository.findByDriverId(driverId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private Vehicle findVehicleOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }

    private VehicleResponseDto mapToResponseDto(Vehicle vehicle) {
        return VehicleResponseDto.builder()
                .id(vehicle.getId())
                .driverId(vehicle.getDriverId())
                .vehicleType(vehicle.getVehicleType())
                .model(vehicle.getModel())
                .plateNumber(vehicle.getPlateNumber())
                .build();
    }
}
