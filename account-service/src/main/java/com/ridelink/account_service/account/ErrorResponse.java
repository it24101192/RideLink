package com.ridelink.account_service.account;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Map<String, String> errors;

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }

    public ErrorResponse(
            int status,
            String error,
            String message,
            Map<String, String> errors) {

        this.status = status;
        this.error = error;
        this.message = message;
        this.errors = errors;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
