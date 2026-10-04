package com.ridelink.drivervehicle.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponseDto {

    private Long id;
    private Long driverId;
    private String vehicleType;
    private String model;
    private String plateNumber;

    public String getVehicleNumber() {
        return plateNumber;
    }
}
