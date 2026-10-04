package com.ridelink.ridemanagement.service;

import com.ridelink.ridemanagement.client.DriverServiceClient;
import com.ridelink.ridemanagement.error.DomainException;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DriverAssignmentService {
    private final DriverServiceClient drivers;
    private final double radiusKm;
    private final boolean serviceAreaCheck;
    public DriverAssignmentService(DriverServiceClient drivers, @Value("${driver.search.radius-km:10}") double radiusKm,
                                  @Value("${driver.service-area-check:false}") boolean serviceAreaCheck) {
        this.drivers = drivers; this.radiusKm = radiusKm; this.serviceAreaCheck = serviceAreaCheck;
    }
    public UUID select(double lat, double lng, String expectedServiceArea, String bearerToken) {
        var available = drivers.available(lat, lng, radiusKm, bearerToken);
        return available.stream()
            .filter(driver -> !serviceAreaCheck || matchesServiceArea(driver.serviceArea(), expectedServiceArea))
            .min(Comparator.comparingDouble(driver -> haversine(lat, lng, driver.lat(), driver.lng())))
            .map(driver -> {
                try { return UUID.fromString(driver.id()); }
                catch (IllegalArgumentException ex) { throw DomainException.unavailable("DRIVER_SERVICE_INVALID_RESPONSE", "Driver Service returned an invalid driver id"); }
            }).orElseThrow(() -> DomainException.conflict("NO_AVAILABLE_DRIVER", "No Available Driver"));
    }
    private static boolean matchesServiceArea(String driverArea, String expectedArea) {
        return driverArea != null && expectedArea != null && !expectedArea.isBlank()
            && driverArea.trim().equalsIgnoreCase(expectedArea.trim());
    }
    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double p1 = Math.toRadians(lat1); double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1); double dl = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 6371.0088 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
