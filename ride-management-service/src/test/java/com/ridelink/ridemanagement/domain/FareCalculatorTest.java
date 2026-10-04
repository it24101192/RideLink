package com.ridelink.ridemanagement.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class FareCalculatorTest {
    private final FareCalculator calculator = new FareCalculator();
    @Test void normalFareUsesDistanceAndDurationRates() {
        assertEquals(new BigDecimal("700.00"), calculator.calculate(5, 10, LocalTime.NOON));
    }
    @Test void fareHasMinimumOfThreeHundred() {
        assertEquals(new BigDecimal("300.00"), calculator.calculate(0.1, 1, LocalTime.NOON));
    }
    @Test void peakFareUsesOnePointFiveMultiplier() {
        assertEquals(new BigDecimal("1050.00"), calculator.calculate(5, 10, LocalTime.of(8, 0)));
    }
    @Test void haversineDistanceIsPositiveForDifferentCoordinates() {
        org.junit.jupiter.api.Assertions.assertTrue(calculator.distanceKm(6.9271, 79.8612, 7.2906, 80.6337) > 0);
    }
}
