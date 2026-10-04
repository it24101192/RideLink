package com.ridelink.drivervehicle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequestDto {

    private Long driverId;

    @NotBlank(message = "Vehicle type is required (e.g., CAR, VAN, BIKE, TUK_TUK)")
    @Size(min = 2, max = 50, message = "Vehicle type must be between 2 and 50 characters")
    private String vehicleType;

    @NotBlank(message = "Vehicle model is required")
    @Size(min = 2, max = 100, message = "Model must be between 2 and 100 characters")
    private String model;

    @NotBlank(message = "Plate / vehicle number is required")
    @Size(min = 3, max = 50, message = "Plate number must be between 3 and 50 characters")
    private String plateNumber;
}
