package com.ridelink.ridemanagement.dto;

import com.ridelink.ridemanagement.entity.Ride;
import com.ridelink.ridemanagement.enums.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponse {

    private Long rideId;
    private Long passengerId;
    private Long driverId;
    private String pickupLocation;
    private String destination;
    private RideStatus status;
    private Double estimatedFare;
    private Double finalFare;
    private LocalDateTime requestedAt;

    public static RideResponse fromEntity(Ride ride) {
        return RideResponse.builder()
                .rideId(ride.getId())
                .passengerId(ride.getPassengerId())
                .driverId(ride.getDriverId())
                .pickupLocation(ride.getPickupLocation())
                .destination(ride.getDestination())
                .status(ride.getStatus())
                .estimatedFare(ride.getEstimatedFare())
                .finalFare(ride.getFinalFare())
                .requestedAt(ride.getRequestedAt())
                .build();
    }
}
