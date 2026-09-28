package com.ridelink.ridemanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignDriverRequest {

    @NotNull(message = "driverId is required")
    private Long driverId;
}
