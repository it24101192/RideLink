package com.ridelink.drivervehicle.dto.request;

import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityUpdateDto {

    @NotNull(message = "Availability status is required (AVAILABLE, BUSY, or OFFLINE)")
    private AvailabilityStatus availability;
}
