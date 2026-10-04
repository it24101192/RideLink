package com.ridelink.ridemanagement.domain;

import com.ridelink.ridemanagement.error.DomainException;
import com.ridelink.ridemanagement.model.RideStatus;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StatusTransitionValidator {
    private static final Map<RideStatus, Set<RideStatus>> ALLOWED = Map.of(
        RideStatus.REQUESTED, Set.of(RideStatus.ASSIGNED, RideStatus.CANCELLED),
        RideStatus.ASSIGNED, Set.of(RideStatus.ACCEPTED, RideStatus.CANCELLED),
        RideStatus.ACCEPTED, Set.of(RideStatus.IN_PROGRESS, RideStatus.CANCELLED),
        RideStatus.IN_PROGRESS, Set.of(RideStatus.COMPLETED),
        RideStatus.COMPLETED, Set.of(), RideStatus.CANCELLED, Set.of());

    public void validate(RideStatus from, RideStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw DomainException.badRequest("INVALID_STATUS_TRANSITION", "Invalid Status Transition: " + from + " -> " + to);
        }
    }
}
