package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.domain.FareCalculator;

public record FinalFareDto(String rideId, FareCalculator.Fare fare) { }
