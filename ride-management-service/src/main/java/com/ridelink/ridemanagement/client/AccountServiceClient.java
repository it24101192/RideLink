package com.ridelink.ridemanagement.client;

import com.ridelink.ridemanagement.error.DomainException;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AccountServiceClient {
    private final RestClient client;
    public AccountServiceClient(RestClient.Builder builder, @Value("${account.service.url}") String baseUrl,
                               @Value("${account.service.connect-timeout:2s}") Duration connectTimeout,
                               @Value("${account.service.read-timeout:3s}") Duration readTimeout) {
        this.client = builder.baseUrl(baseUrl).build();
    }
    public void validateUser(String userId, String requiredRole, String bearerToken) {
        try {
            Map<?, ?> response = client.get().uri(uri -> uri.path("/api/users/{userId}").build(userId))
                .header("Authorization", bearerToken).retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { throw DomainException.forbidden("Account Service rejected user identity"); })
                .body(Map.class);
            Object rawRole = response == null ? null : response.get("role");
            if (rawRole == null || !requiredRole.equalsIgnoreCase(String.valueOf(rawRole).replaceFirst("^ROLE_", ""))) {
                throw DomainException.forbidden("Account Service user role does not match required role");
            }
            Object status = response.get("status");
            if (status != null && !"ACTIVE".equalsIgnoreCase(String.valueOf(status))) {
                throw DomainException.forbidden("Account Service user is not active");
            }
        } catch (DomainException ex) { throw ex; }
        catch (RestClientException ex) { throw DomainException.unavailable("ACCOUNT_SERVICE_UNAVAILABLE", "Account Service is unavailable"); }
    }
}
