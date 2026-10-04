package com.ridelink.farepayment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;


public final class FareCalculator {
    private static final long BASE_FARE_CENTS = 20_000L;
    private static final long PER_KM_CENTS = 8_000L;
    private static final long PER_MINUTE_CENTS = 1_000L;
    private static final long MINIMUM_FARE_CENTS = 30_000L;
    private static final BigDecimal DEFAULT_SURGE = BigDecimal.ONE;
    private static final BigDecimal MAX_SURGE = new BigDecimal("2.5");

    public Fare calculate(Input input) {
        Objects.requireNonNull(input, "input must not be null");
        validate(input);

        long distanceFare = amount(input.distanceKm(), PER_KM_CENTS);
        long timeFare = amount(input.durationMinutes(), PER_MINUTE_CENTS);
        long subtotal = Math.addExact(Math.addExact(BASE_FARE_CENTS, distanceFare), timeFare);
        long surgedTotal = new BigDecimal(subtotal)
            .multiply(input.surgeMultiplier() == null ? DEFAULT_SURGE : input.surgeMultiplier())
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
        long surgeAmount = surgedTotal - subtotal;
        boolean minimumApplied = surgedTotal < MINIMUM_FARE_CENTS;
        long totalFare = minimumApplied ? MINIMUM_FARE_CENTS : surgedTotal;

        return new Fare(BASE_FARE_CENTS, distanceFare, timeFare, surgeAmount,
            subtotal, minimumApplied, totalFare, "LKR",
            "base + distance + time; surge applied to subtotal; LKR 300 minimum");
    }

    private static long amount(double quantity, long rateCents) {
        return BigDecimal.valueOf(quantity)
            .multiply(BigDecimal.valueOf(rateCents))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    private static void validate(Input input) {
        if (!Double.isFinite(input.distanceKm()) || input.distanceKm() < 0) {
            throw new IllegalArgumentException("distanceKm must be a finite, non-negative number");
        }
        if (!Double.isFinite(input.durationMinutes()) || input.durationMinutes() < 0) {
            throw new IllegalArgumentException("durationMinutes must be a finite, non-negative number");
        }
        BigDecimal surge = input.surgeMultiplier() == null ? DEFAULT_SURGE : input.surgeMultiplier();
        if (surge.compareTo(BigDecimal.ONE) < 0 || surge.compareTo(MAX_SURGE) > 0) {
            throw new IllegalArgumentException("surgeMultiplier must be between 1.0 and 2.5");
        }
    }

    public record Input(double distanceKm, double durationMinutes, BigDecimal surgeMultiplier) {
        public Input(double distanceKm, double durationMinutes, double surgeMultiplier) {
            this(distanceKm, durationMinutes, BigDecimal.valueOf(surgeMultiplier));
        }

        public Input(double distanceKm, double durationMinutes) {
            this(distanceKm, durationMinutes, null);
        }
    }


    public record Fare(long baseFare, long distanceFare, long timeFare,
                       long surgeAmount, long subtotal, boolean minimumApplied,
                       long totalFare, String currency, String breakdown) { }
}
