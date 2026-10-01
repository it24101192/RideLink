package com.ridelink.ridemanagement.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiErrorResponse> domain(DomainException ex) {
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(ApiExceptionHandler::formatFieldError).toList();
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request validation failed", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class})
    ResponseEntity<ApiErrorResponse> malformed(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> internal(Exception ex) {
        LOGGER.error("Unexpected error handling ride request", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred", List.of());
    }

    private static String formatFieldError(FieldError error) { return error.getField() + ": " + error.getDefaultMessage(); }
    private static ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message, List<String> details) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(code, message, details));
    }
}
