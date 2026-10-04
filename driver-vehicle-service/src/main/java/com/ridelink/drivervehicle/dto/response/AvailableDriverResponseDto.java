package com.ridelink.drivervehicle.dto.response;

import lombok.*;

/**
 * DTO returned to Ride Management Service (Member 3) by
 * GET /api/drivers/available?lat=..&lng=..&radius=..
 *
 * Field names match ride-management-service's DriverServiceClient.AvailableDriver
 * record: (String id, double lat, double lng, String serviceArea).
 * "id" is the driver's Account Service user id, because Ride Management uses it
 * as the ride's driverId and validates it against Account Service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableDriverResponseDto {

    private String id;
    private Long driverId;
    private double lat;
    private double lng;
    private String serviceArea;
    private Double distanceKm;
}
