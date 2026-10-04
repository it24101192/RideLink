package com.ridelink.drivervehicle.dto.response;

import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO returned to Ride Management Service (Member 3)
 * when querying eligible available drivers in a service area.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibleDriverResponseDto {

    private Long driverId;
    private UUID accountId;
    private String driverName;
    private Long vehicleId;
    private String vehicleNumber;
    private String vehicleType;
    private AvailabilityStatus availability;
    private String serviceArea;
    private Double latitude;
    private Double longitude;
    private LocalDateTime locationUpdatedAt;
}
