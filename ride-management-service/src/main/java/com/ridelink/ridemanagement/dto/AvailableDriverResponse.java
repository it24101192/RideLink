package com.ridelink.ridemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Shape of the response expected FROM the Driver & Vehicle Service
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDriverResponse {
    private Long driverId;
    private String driverName;
    private String vehicleNumber;
    private String currentLocation;
}
