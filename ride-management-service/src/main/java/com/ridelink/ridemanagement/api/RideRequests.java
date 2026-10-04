package com.ridelink.ridemanagement.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RideRequests {
    private RideRequests() { }
    public record CreateRideRequest(@NotNull @Min(-90) @Max(90) Double pickupLat,
                                    @NotNull @Min(-180) @Max(180) Double pickupLng,
                                    @NotBlank @Size(max = 300) String pickupAddress,
                                    @NotNull @Min(-90) @Max(90) Double destLat,
                                    @NotNull @Min(-180) @Max(180) Double destLng,
                                    @NotBlank @Size(max = 300) String destAddress,
                                    @Min(1) @Max(600) Integer durationMin,
                                    @Size(max = 100) String serviceArea) { }
    public record CancelRequest(@NotBlank @Size(max = 500) String reason) { }
    public record AssignRequest(@NotNull java.util.UUID driverId) { }
}
