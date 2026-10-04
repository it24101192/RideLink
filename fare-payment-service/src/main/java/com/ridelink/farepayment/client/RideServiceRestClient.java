package com.ridelink.farepayment.client;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.net.http.HttpClient;
import com.ridelink.farepayment.error.RideServiceUnavailableError;

    /** Synchronous adapter for GET {RIDE_SERVICE_URL}/api/rides/{rideId}. */
@Component
@ConditionalOnProperty(name = "ride.service.client.enabled", havingValue = "true", matchIfMissing = true)
public class RideServiceRestClient implements RideServiceClient {
    private final RestClient client;
    private final int retryCount;
    private final Duration retryDelay;

    @Autowired
    public RideServiceRestClient(RestClient.Builder builder,
            @Value("${ride.service.url}") String baseUrl,
            @Value("${ride.service.connect-timeout:2s}") Duration connectTimeout,
            @Value("${ride.service.read-timeout:3s}") Duration readTimeout,
            @Value("${ride.service.retry-count:2}") int retryCount,
            @Value("${ride.service.retry-delay:100ms}") Duration retryDelay) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.retryCount = Math.max(0, retryCount);
        this.retryDelay = retryDelay;
    }

    /** Injectable transport constructor for unit tests. */
    public RideServiceRestClient(RestClient client, int retryCount, Duration retryDelay) {
        this.client = client;
        this.retryCount = Math.max(0, retryCount);
        this.retryDelay = retryDelay;
    }

    @Override
    public Optional<RideServiceRide> getRide(UUID rideId, String bearerToken) {
        RestClientException lastFailure = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                RideServiceRide ride = client.get()
                    .uri("/api/rides/{rideId}", rideId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(RideServiceRide.class);
                if (ride == null) throw new RestClientException("Ride Service returned an empty response");
                if (ride.id() == null || !rideId.equals(ride.id())) {
                    throw new RestClientException("Ride Service returned a mismatched ride identifier");
                }
                return Optional.of(ride);
            } catch (HttpClientErrorException.NotFound notFound) {
                return Optional.empty();
            } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
                throw new com.ridelink.farepayment.error.UnauthorizedError("Ride Management rejected the payment request credentials");
            } catch (HttpClientErrorException ex) {
                throw new com.ridelink.farepayment.error.ValidationError("Ride Management rejected the ride lookup request");
            } catch (ResourceAccessException | HttpServerErrorException ex) {
                lastFailure = ex;
                if (attempt < retryCount) pauseBeforeRetry(attempt);
            } catch (RestClientException ex) {
                throw new RideServiceUnavailableError("Ride Management Service request failed", ex);
            }
        }
        throw new RideServiceUnavailableError(
            "Ride Management Service is unavailable after " + (retryCount + 1) + " attempts", lastFailure);
    }

    private void pauseBeforeRetry(int attempt) {
        long nanos = retryDelay.toNanos() * (attempt + 1L);
        LockSupport.parkNanos(nanos);
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new RideServiceUnavailableError("Ride Management Service retry was interrupted");
        }
    }
}
