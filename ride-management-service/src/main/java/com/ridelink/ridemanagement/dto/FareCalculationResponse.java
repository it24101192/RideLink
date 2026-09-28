package com.ridelink.ridemanagement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FareCalculationResponse {
    private Long rideId;
    private Double finalFare;
    private String receiptId;
}
