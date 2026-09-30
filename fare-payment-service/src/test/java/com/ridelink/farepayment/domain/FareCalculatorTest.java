package com.ridelink.farepayment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FareCalculatorTest {
    private final FareCalculator calculator = new FareCalculator();

    @Test
    void calculatesNormalFareInIntegerCents() {
        var fare = calculator.calculate(new FareCalculator.Input(10.5, 20));

        assertEquals(20_000, fare.baseFare());
        assertEquals(84_000, fare.distanceFare());
        assertEquals(20_000, fare.timeFare());
        assertEquals(124_000, fare.subtotal());
        assertEquals(0, fare.surgeAmount());
        assertEquals(124_000, fare.totalFare());
        assertEquals("LKR", fare.currency());
        assertFalse(fare.minimumApplied());
    }

    @Test
    void appliesMinimumFareWhenSubtotalIsBelowMinimum() {
        var fare = calculator.calculate(new FareCalculator.Input(0, 0));

        assertEquals(20_000, fare.subtotal());
        assertEquals(30_000, fare.totalFare());
        assertTrue(fare.minimumApplied());
    }

    @Test
    void appliesSurgeToSubtotal() {
        var fare = calculator.calculate(new FareCalculator.Input(5, 10, 1.5));

        assertEquals(70_000, fare.subtotal());
        assertEquals(35_000, fare.surgeAmount());
        assertEquals(105_000, fare.totalFare());
        assertFalse(fare.minimumApplied());
    }

    @Test
    void acceptsZeroDistanceAndDurationAndUsesDefaultSurge() {
        var fare = calculator.calculate(new FareCalculator.Input(0, 0));

        assertEquals(20_000, fare.distanceFare());
        assertEquals(0, fare.timeFare());
        assertEquals(0, fare.surgeAmount());
        assertEquals(30_000, fare.totalFare());
    }

    @Test
    void rejectsNegativeDistanceOrDuration() {
        assertThrows(IllegalArgumentException.class,
            () -> calculator.calculate(new FareCalculator.Input(-0.1, 2)));
        assertThrows(IllegalArgumentException.class,
            () -> calculator.calculate(new FareCalculator.Input(2, -1)));
    }

    @Test
    void rejectsSurgeOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class,
            () -> calculator.calculate(new FareCalculator.Input(1, 1, 0.9)));
        assertThrows(IllegalArgumentException.class,
            () -> calculator.calculate(new FareCalculator.Input(1, 1, 2.6)));
    }
}
