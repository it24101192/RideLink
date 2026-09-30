package com.ridelink.farepayment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HealthControllerTest {
    @Test
    void reportsServiceAsUp() {
        var response = new HealthController().health();

        assertEquals("UP", response.get("status"));
        assertEquals("fare-payment-service", response.get("service"));
    }
}
