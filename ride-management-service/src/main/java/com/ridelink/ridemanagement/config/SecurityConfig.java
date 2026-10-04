package com.ridelink.ridemanagement.config;

import tools.jackson.databind.ObjectMapper;
import com.ridelink.ridemanagement.error.ApiErrorResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("RIDE_JWT_SECRET must be at least 32 bytes");
        MacAlgorithm algorithm = bytes.length >= 64 ? MacAlgorithm.HS512 : bytes.length >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        String jcaAlgorithm = bytes.length >= 64 ? "HmacSHA512" : bytes.length >= 48 ? "HmacSHA384" : "HmacSHA256";
        SecretKey key = new SecretKeySpec(bytes, jcaAlgorithm);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(algorithm).build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFrom);
        return converter;
    }

    private Collection<GrantedAuthority> authoritiesFrom(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        String role = jwt.getClaimAsString("role");
        if (role != null) authorities.add(new SimpleGrantedAuthority(authorityName(role)));
        var roles = jwt.getClaimAsStringList("roles");
        if (roles != null) roles.forEach(value -> authorities.add(new SimpleGrantedAuthority(authorityName(value))));
        return authorities;
    }

    private static String authorityName(String role) {
        String normalized = role.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT);
        if ("RIDER".equals(normalized)) normalized = "PASSENGER";
        return "ROLE_" + normalized;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        var unauthorized = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, ex) -> {
            response.setStatus(401); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ApiErrorResponse.of("UNAUTHORIZED", "Authentication is required", java.util.List.of()));
        };
        var forbidden = (org.springframework.security.web.access.AccessDeniedHandler) (request, response, ex) -> {
            response.setStatus(403); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ApiErrorResponse.of("FORBIDDEN", "You do not have permission to access this resource", java.util.List.of()));
        };
        return http.csrf(csrf -> csrf.disable())
                .cors(withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.requestMatchers("/health", "/api-docs", "/api-docs/**", "/openapi.yaml", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/rides/*/assign").hasAnyRole("ADMIN", "SYSTEM")
                .requestMatchers("/api/rides/*/accept", "/api/rides/*/start", "/api/rides/*/complete").hasAnyRole("DRIVER", "ADMIN")
                .requestMatchers("/api/rides/*/cancel").hasAnyRole("PASSENGER", "DRIVER", "ADMIN")
                .requestMatchers("/api/rides/fare-estimate").hasAnyRole("PASSENGER", "ADMIN")
                .requestMatchers("/api/rides").hasAnyRole("PASSENGER", "DRIVER", "ADMIN")
                .requestMatchers("/api/rides/**").hasAnyRole("PASSENGER", "DRIVER", "ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
            .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(unauthorized).jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
    }
}
