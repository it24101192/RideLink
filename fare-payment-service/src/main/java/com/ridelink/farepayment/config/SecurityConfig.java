package com.ridelink.farepayment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.ridelink.farepayment.error.ApiErrorResponse;
import com.ridelink.farepayment.security.RequestLoggingFilter;

@Configuration
public class SecurityConfig {
    private final ObjectMapper objectMapper;
    private final RequestLoggingFilter requestLoggingFilter;

    public SecurityConfig(ObjectMapper objectMapper, RequestLoggingFilter requestLoggingFilter) {
        this.objectMapper = objectMapper;
        this.requestLoggingFilter = requestLoggingFilter;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFrom);
        return converter;
    }

    private Collection<GrantedAuthority> authoritiesFrom(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isBlank()) {
            authorities.add(new SimpleGrantedAuthority(authorityName(role)));
        }
        var roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.forEach(value -> authorities.add(new SimpleGrantedAuthority(authorityName(value))));
        }
        return authorities;
    }

    private static String authorityName(String role) {
        String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
        normalized = normalized.toUpperCase(Locale.ROOT);
        if ("RIDER".equals(normalized)) normalized = "PASSENGER";
        return "ROLE_" + normalized;
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String configuredSecret) {
        if (configuredSecret == null || configuredSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("FARE_JWT_SECRET must be at least 32 bytes");
        }
        int keyLength = configuredSecret.getBytes(StandardCharsets.UTF_8).length;
        MacAlgorithm algorithm = keyLength >= 64 ? MacAlgorithm.HS512
            : keyLength >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        String jcaAlgorithm = keyLength >= 64 ? "HmacSHA512"
            : keyLength >= 48 ? "HmacSHA384" : "HmacSHA256";
        SecretKey key = new SecretKeySpec(configuredSecret.getBytes(StandardCharsets.UTF_8), jcaAlgorithm);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(algorithm).build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/api-docs", "/api-docs/**", "/v3/api-docs/**",
                    "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/v1/payments/*/refund").hasRole("ADMIN")
                .requestMatchers("/api/v1/**").hasAnyRole("PASSENGER", "DRIVER", "ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler()))
            .oauth2ResourceServer(oauth -> oauth
                .authenticationEntryPoint(authenticationEntryPoint())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .addFilterAfter(requestLoggingFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class)
            .build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            objectMapper.writeValue(response.getOutputStream(),
                ApiErrorResponse.of("UNAUTHORIZED", "Authentication is required", java.util.List.of()));
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            response.setStatus(403);
            response.setContentType("application/json");
            objectMapper.writeValue(response.getOutputStream(),
                ApiErrorResponse.of("FORBIDDEN", "You do not have permission to access this resource", java.util.List.of()));
        };
    }
}
