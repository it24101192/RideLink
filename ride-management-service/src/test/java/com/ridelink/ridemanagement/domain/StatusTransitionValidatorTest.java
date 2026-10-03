package com.ridelink.ridemanagement.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.ridelink.ridemanagement.error.DomainException;
import com.ridelink.ridemanagement.model.RideStatus;
import org.junit.jupiter.api.Test;

class StatusTransitionValidatorTest {
    private final StatusTransitionValidator validator = new StatusTransitionValidator();
    @Test void permitsEverySpecifiedTransition() {
        assertDoesNotThrow(() -> validator.validate(RideStatus.REQUESTED, RideStatus.ASSIGNED));
        assertDoesNotThrow(() -> validator.validate(RideStatus.REQUESTED, RideStatus.CANCELLED));
        assertDoesNotThrow(() -> validator.validate(RideStatus.ASSIGNED, RideStatus.ACCEPTED));
        assertDoesNotThrow(() -> validator.validate(RideStatus.ASSIGNED, RideStatus.CANCELLED));
        assertDoesNotThrow(() -> validator.validate(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS));
        assertDoesNotThrow(() -> validator.validate(RideStatus.ACCEPTED, RideStatus.CANCELLED));
        assertDoesNotThrow(() -> validator.validate(RideStatus.IN_PROGRESS, RideStatus.COMPLETED));
    }
    @Test void rejectsAnyUnspecifiedTransition() {
        assertThrows(DomainException.class, () -> validator.validate(RideStatus.REQUESTED, RideStatus.COMPLETED));
        assertThrows(DomainException.class, () -> validator.validate(RideStatus.COMPLETED, RideStatus.CANCELLED));
        assertThrows(DomainException.class, () -> validator.validate(RideStatus.CANCELLED, RideStatus.REQUESTED));
    }
}
