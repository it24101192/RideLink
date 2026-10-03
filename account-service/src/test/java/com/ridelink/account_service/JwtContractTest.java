package com.ridelink.account_service;

import com.ridelink.account_service.account.Account;
import com.ridelink.account_service.account.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Contract test: tokens issued by Account Service must be accepted by
 * ride-management-service and fare-payment-service. Both build their decoder as below
 * (HMAC, algorithm chosen from secret length) and read the "userId" (UUID) and "role" claims.
 */
class JwtContractTest {

    private static final String SECRET = "replace-with-at-least-32-random-characters";

    @Test
    void tokenIsAcceptedByRideAndFareServiceDecoder() {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("kamal");
        account.setRole("DRIVER");
        account.setStatus("ACTIVE");

        String token = new JwtService(SECRET).generateToken(account);

        Jwt jwt = rideServiceDecoder(SECRET).decode(token);

        String userId = String.valueOf(jwt.getClaims().getOrDefault("userId", jwt.getSubject()));
        assertEquals(account.getId(), UUID.fromString(userId));
        assertEquals("DRIVER", jwt.getClaimAsString("role"));
        assertNotNull(jwt.getExpiresAt());
    }

    /** Same logic as SecurityConfig.jwtDecoder in ride-management-service / fare-payment-service. */
    private static NimbusJwtDecoder rideServiceDecoder(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        MacAlgorithm algorithm = bytes.length >= 64 ? MacAlgorithm.HS512
                : bytes.length >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        String jca = bytes.length >= 64 ? "HmacSHA512" : bytes.length >= 48 ? "HmacSHA384" : "HmacSHA256";
        SecretKey key = new SecretKeySpec(bytes, jca);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(algorithm).build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }
}
