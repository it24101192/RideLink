package com.ridelink.ridemanagement.client;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import com.ridelink.ridemanagement.error.DomainException;

@Component
public class FarePaymentClient {
    private final RestClient client;

    @Autowired
    public FarePaymentClient(RestClient.Builder builder, @Value("${fare.payment.service.url}") String baseUrl,
            @Value("${fare.payment.service.connect-timeout:2s}") Duration connectTimeout,
            @Value("${fare.payment.service.read-timeout:3s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public FarePaymentClient(RestClient client) {
        this.client = client;
    }

    public List<PaymentRecord> getByRideId(UUID rideId, String bearerToken) {
        try {
            List<PaymentRecord> records = client.get().uri("/api/v1/payments/ride/{rideId}", rideId)
                    .header("Authorization", bearerToken).retrieve()
                    .onStatus(status -> status.value() == 401 || status.value() == 403,
                            (request, response) -> { throw DomainException.forbidden("Fare & Payment rejected the request identity"); })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            (request, response) -> { throw DomainException.unavailable("FARE_PAYMENT_UNAVAILABLE", "Fare & Payment Service is unavailable"); })
                    .body(new ParameterizedTypeReference<List<PaymentRecord>>() { });
            return records == null ? List.of() : records;
        } catch (DomainException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) return List.of();
            throw DomainException.unavailable("FARE_PAYMENT_ERROR", "Fare & Payment Service could not return payment details");
        } catch (RestClientException exception) {
            throw DomainException.unavailable("FARE_PAYMENT_UNAVAILABLE", "Fare & Payment Service is unavailable");
        }
    }

    public record PaymentRecord(UUID rideId, int amount, String currency, String status,
                                String paymentMethod, String transactionRef, LocalDateTime createdAt) { }
}
