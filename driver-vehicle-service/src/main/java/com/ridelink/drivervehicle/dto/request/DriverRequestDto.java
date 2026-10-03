package com.ridelink.drivervehicle.dto.request;

import com.ridelink.drivervehicle.entity.AvailabilityStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverRequestDto {

    /** Account Service user id (UUID). */
    private UUID accountId;

    @NotBlank(message = "Driver name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone number must be a valid numeric phone number (9-15 digits, optional leading +)")
    private String phone;

    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "License number is required")
    @Size(min = 3, max = 50, message = "License number must be between 3 and 50 characters")
    private String licenseNo;

    private AvailabilityStatus availability;

    private String serviceArea;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private Double longitude;
}
