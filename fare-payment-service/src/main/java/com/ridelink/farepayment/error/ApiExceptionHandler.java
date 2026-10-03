package com.ridelink.farepayment.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

    @ExceptionHandler(ValidationError.class)
    ResponseEntity<ApiErrorResponse> validation(ValidationError ex) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", ex.getMessage(), List.of());
    }

    @ExceptionHandler(NotFoundError.class)
    ResponseEntity<ApiErrorResponse> notFound(NotFoundError ex) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), List.of());
    }

    @ExceptionHandler(PaymentFailedError.class)
    ResponseEntity<ApiErrorResponse> paymentFailed(PaymentFailedError ex) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_FAILED", ex.getMessage(), List.of());
    }

    @ExceptionHandler(RideNotCompletedError.class)
    ResponseEntity<ApiErrorResponse> rideNotCompleted(RideNotCompletedError ex) {
        return response(HttpStatus.CONFLICT, "RIDE_NOT_COMPLETED", ex.getMessage(), List.of());
    }

    @ExceptionHandler(DuplicatePaymentError.class)
    ResponseEntity<ApiErrorResponse> duplicatePayment(DuplicatePaymentError ex) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", ex.getMessage(), List.of());
    }

    @ExceptionHandler(RideServiceUnavailableError.class)
    ResponseEntity<ApiErrorResponse> rideServiceUnavailable(RideServiceUnavailableError ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "RIDE_SERVICE_UNAVAILABLE", ex.getMessage(), List.of());
    }

    @ExceptionHandler(UnauthorizedError.class)
    ResponseEntity<ApiErrorResponse> forbidden(UnauthorizedError ex) {
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> invalidBody(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(ApiExceptionHandler::formatFieldError).toList();
        boolean semantic = ex.getBindingResult().getFieldErrors().stream().anyMatch(error ->
            List.of("distanceKm", "actualDistanceKm", "durationMinutes", "actualDurationMinutes",
                "surgeMultiplier").contains(error.getField()));
        return semantic
            ? response(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Request validation failed", details)
            : response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request validation failed", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> malformedRequest(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> conflict(DataIntegrityViolationException ex) {
        return response(HttpStatus.CONFLICT, "CONFLICT", "Request conflicts with an existing record", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> internalError(Exception ex) {
        LOGGER.error("Unexpected error while handling API request", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred", List.of());
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private static ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code,
                                                               String message, List<String> details) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(code, message, details));
    }
}
