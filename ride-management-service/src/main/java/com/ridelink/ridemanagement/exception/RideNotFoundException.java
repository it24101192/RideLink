package com.ridelink.ridemanagement.exception;

public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(Long id) {
        super("Ride not found with id: " + id);
    }
}
