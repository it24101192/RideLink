package com.ridelink.ridemanagement.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Synchronous REST client that calls the Fare & Payment Service to
 * get a fare estimate at ride-creation time, and to trigger final
 * fare calculation once a ride completes.
 */
@Component
public class FarePaymentServiceClient {

    private final RestClient restClient;

    public FarePaymentServiceClient(@Value("${services.fare-payment-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Double getFareEstimate(String pickup, String destination) {
        Map<?, ?> response = restClient.post()
                .uri("/api/fares/estimate")
                .body(Map.of("pickup", pickup, "destination", destination))
                .retrieve()
                .body(Map.class);
        Object fare = response != null ? response.get("estimatedFare") : null;
        return fare != null ? Double.valueOf(fare.toString()) : null;
    }

    public void triggerFinalFareCalculation(Long rideId, Long passengerId, Long driverId) {
        restClient.post()
                .uri("/api/fares/finalize")
                .body(Map.of("rideId", rideId, "passengerId", passengerId, "driverId", driverId))
                .retrieve()
                .toBodilessEntity();
    }
}
