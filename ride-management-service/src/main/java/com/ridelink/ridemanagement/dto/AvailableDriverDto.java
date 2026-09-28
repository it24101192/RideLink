package com.ridelink.ridemanagement.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Mirrors the response shape from Driver & Vehicle Service's
 * GET /api/drivers/available endpoint. Kept as a lightweight local
 * DTO so this service never touches that service's database directly.
 */
@Getter
@Setter
public class AvailableDriverDto {
    private Long driverId;
    private String currentLocation;
    private String vehicleType;
    private Double rating;
}
