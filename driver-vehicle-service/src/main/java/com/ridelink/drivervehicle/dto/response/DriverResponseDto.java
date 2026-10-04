package com.ridelink.drivervehicle.dto.response;

import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponseDto {

    private Long id;
    private UUID accountId;
    private String name;
    private String phone;
    private String email;
    private String licenseNo;
    private AvailabilityStatus availability;
    private String serviceArea;
    private Double latitude;
    private Double longitude;
    private LocalDateTime locationUpdatedAt;
    private List<VehicleResponseDto> vehicles;
}
