package com.ridelink.ridemanagement.error;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public DomainException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public static DomainException badRequest(String code, String message) { return new DomainException(HttpStatus.BAD_REQUEST, code, message); }
    public static DomainException forbidden(String message) { return new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", message); }
    public static DomainException notFound(String message) { return new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", message); }
    public static DomainException conflict(String code, String message) { return new DomainException(HttpStatus.CONFLICT, code, message); }
    public static DomainException unavailable(String code, String message) { return new DomainException(HttpStatus.SERVICE_UNAVAILABLE, code, message); }
}
