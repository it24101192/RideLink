package com.ridelink.ridemanagement.client;

import com.ridelink.ridemanagement.dto.AvailableDriverResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Synchronous REST client that calls the Driver & Vehicle Service.
 * Chosen as SYNCHRONOUS because ride assignment needs an up-to-date,
 * real-time list of available drivers before the ride can proceed —
 * an async/eventual-consistency approach would risk assigning a driver
 * who is no longer available.
 */
@Component
public class DriverServiceClient {

    private final RestClient restClient;

    public DriverServiceClient(@Value("${services.driver-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<AvailableDriverResponse> getAvailableDrivers(String location) {
        return restClient.get()
                .uri("/api/drivers/available?location={location}", location)
                .retrieve()
                .body(List.class); // In production, map to List<AvailableDriverResponse> with a ParameterizedTypeReference
    }
}
