package com.ridelink.farepayment.error;

import java.util.List;

public record ApiErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, List<String> details) { }

    public static ApiErrorResponse of(String code, String message, List<String> details) {
        return new ApiErrorResponse(new ErrorBody(code, message, details == null ? List.of() : details));
    }
}
