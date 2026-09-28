package com.ridelink.ridemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRideRequest {

    @NotNull(message = "passengerId is required")
    private Long passengerId;

    @NotBlank(message = "pickup location is required")
    private String pickupLocation;

    @NotBlank(message = "destination is required")
    private String destination;
}
