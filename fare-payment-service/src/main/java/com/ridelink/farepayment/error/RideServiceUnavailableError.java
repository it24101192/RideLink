package com.ridelink.farepayment.error;

public class RideServiceUnavailableError extends DomainError {
    public RideServiceUnavailableError(String message) { super(message); }
    public RideServiceUnavailableError(String message, Throwable cause) {
        super(message, cause);
    }
}
