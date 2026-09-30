package com.ridelink.farepayment.error;

public abstract class DomainError extends RuntimeException {
    protected DomainError(String message) { super(message); }
    protected DomainError(String message, Throwable cause) { super(message, cause); }
}
