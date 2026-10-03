package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.dto.request.VehicleRequestDto;
import com.ridelink.drivervehicle.dto.response.VehicleResponseDto;

import java.util.List;

public interface VehicleService {

    VehicleResponseDto createVehicle(VehicleRequestDto request);

    List<VehicleResponseDto> getAllVehicles();

    VehicleResponseDto getVehicleById(Long id);

    VehicleResponseDto updateVehicle(Long id, VehicleRequestDto request);

    void deleteVehicle(Long id);

    VehicleResponseDto assignVehicleToDriver(Long vehicleId, Long driverId);

    List<VehicleResponseDto> getVehiclesByDriverId(Long driverId);
}
