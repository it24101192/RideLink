package com.ridelink.drivervehicle.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
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
        if (bytes.length < 32) {
            throw new IllegalArgumentException("DRIVER_JWT_SECRET must be at least 32 bytes");
        }
        MacAlgorithm algorithm = bytes.length >= 64 ? MacAlgorithm.HS512
                : bytes.length >= 48 ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        String jcaAlgorithm = bytes.length >= 64 ? "HmacSHA512"
                : bytes.length >= 48 ? "HmacSHA384" : "HmacSHA256";
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
        String status = jwt.getClaimAsString("status");
        if (status != null && !"ACTIVE".equalsIgnoreCase(status)) return authorities;
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
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/drivers/available", "/api/drivers/eligible")
                            .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/drivers/*")
                            .hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/drivers/*/availability", "/api/drivers/*/location")
                            .hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/drivers/*/availability", "/api/drivers/*/location")
                            .hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/driver/*", "/api/vehicles/*")
                            .hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
