package com.ridelink.ridemanagement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FareCalculationRequest {
    private Long rideId;
    private Long passengerId;
    private String pickup;
    private String destination;

    public FareCalculationRequest(Long rideId, Long passengerId, String pickup, String destination) {
        this.rideId = rideId;
        this.passengerId = passengerId;
        this.pickup = pickup;
        this.destination = destination;
    }
}
