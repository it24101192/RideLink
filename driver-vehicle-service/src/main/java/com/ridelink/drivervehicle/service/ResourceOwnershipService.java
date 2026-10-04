package com.ridelink.drivervehicle.service;

import com.ridelink.drivervehicle.entity.Driver;
import com.ridelink.drivervehicle.entity.Vehicle;
import com.ridelink.drivervehicle.exception.ResourceNotFoundException;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceOwnershipService {
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;

    public void requireDriverOwner(Long driverId, Authentication authentication) {
        Driver driver = drivers.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
        requireOwner(driver, authentication);
    }

    public void requireVehicleOwner(Long vehicleId, Authentication authentication) {
        Vehicle vehicle = vehicles.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));
        if (vehicle.getDriverId() == null) throw new AccessDeniedException("Vehicle is not assigned to a driver");
        Driver driver = drivers.findById(vehicle.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + vehicle.getDriverId()));
        requireOwner(driver, authentication);
    }

    private static void requireOwner(Driver driver, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return;
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt) || driver.getAccountId() == null) {
            throw new AccessDeniedException("Driver profile access is not allowed");
        }
        String userId = jwt.getClaimAsString("userId");
        if (userId == null || !driver.getAccountId().equals(parseUserId(userId))) {
            throw new AccessDeniedException("Driver profile access is not allowed");
        }
    }

    private static UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
