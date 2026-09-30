package com.ridelink.farepayment.messaging;

import com.ridelink.farepayment.dto.FinalFareDto;

public record FinalFareCalculatedEvent(String eventName, FinalFareDto finalFare) { }
