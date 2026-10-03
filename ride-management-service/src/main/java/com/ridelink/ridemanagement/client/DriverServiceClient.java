package com.ridelink.ridemanagement.client;

import com.ridelink.ridemanagement.error.DomainException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DriverServiceClient {
    private final RestClient client;
    public DriverServiceClient(RestClient.Builder builder, @Value("${driver.service.url}") String baseUrl,
                               @Value("${driver.service.connect-timeout:2s}") Duration connectTimeout,
                               @Value("${driver.service.read-timeout:3s}") Duration readTimeout) {
        this.client = builder.baseUrl(baseUrl).build();
    }
    public List<AvailableDriver> available(double lat, double lng, double radiusKm, String bearerToken) {
        try {
            AvailableDriver[] result = client.get().uri(uri -> uri.path("/api/drivers/available")
                    .queryParam("lat", lat).queryParam("lng", lng).queryParam("radius", radiusKm).build())
                .header("Authorization", bearerToken).retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw DomainException.unavailable("DRIVER_SERVICE_ERROR", "Driver Service could not find available drivers"); })
                .body(AvailableDriver[].class);
            return result == null ? List.of() : List.of(result);
        } catch (DomainException ex) { throw ex; }
        catch (RestClientException ex) { throw DomainException.unavailable("DRIVER_SERVICE_UNAVAILABLE", "Driver Service is unavailable"); }
    }
    public record AvailableDriver(String id, double lat, double lng, String serviceArea) { }
}
