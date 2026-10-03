package com.ridelink.ridemanagement.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class FareCalculator {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final BigDecimal BASE_FARE = new BigDecimal("200.00");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("50.00");
    private static final BigDecimal PER_MIN_RATE = new BigDecimal("5.00");
    private static final BigDecimal MINIMUM_FARE = new BigDecimal("300.00");
    private static final ZoneId LOCAL_ZONE = ZoneId.of("Asia/Colombo");

    public double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double p1 = Math.toRadians(lat1); double p2 = Math.toRadians(lat2);
        double deltaP = Math.toRadians(lat2 - lat1); double deltaL = Math.toRadians(lng2 - lng1);
        double a = Math.sin(deltaP / 2) * Math.sin(deltaP / 2)
            + Math.cos(p1) * Math.cos(p2) * Math.sin(deltaL / 2) * Math.sin(deltaL / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Fare rule: base + distance * rate + duration * rate + surge multiplier; minimum LKR 300. */
    public BigDecimal calculate(double distanceKm, int durationMin, LocalTime localTime) {
        BigDecimal surge = isPeak(localTime) ? new BigDecimal("1.50") : BigDecimal.ONE;
        BigDecimal variable = PER_KM_RATE.multiply(BigDecimal.valueOf(distanceKm))
            .add(PER_MIN_RATE.multiply(BigDecimal.valueOf(durationMin)));
        BigDecimal fare = BASE_FARE.add(variable).multiply(surge).setScale(2, RoundingMode.HALF_UP);
        return fare.max(MINIMUM_FARE);
    }

    public BigDecimal calculateNow(double distanceKm, int durationMin) {
        return calculate(distanceKm, durationMin, LocalTime.now(LOCAL_ZONE));
    }

    public static boolean isPeak(LocalTime time) {
        return (!time.isBefore(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(9, 0)))
            || (!time.isBefore(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(19, 0)));
    }
}
