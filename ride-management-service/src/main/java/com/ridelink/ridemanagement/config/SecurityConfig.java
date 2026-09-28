package com.ridelink.ridemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security setup. Replace with real JWT validation that reads
 * the token issued by the Account Service (shared secret / public key),
 * then map roles (PASSENGER, DRIVER, ADMIN) onto the endpoints below.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // TODO: tighten once JWT filter validating Account Service
                // tokens is wired in, e.g.:
                // .requestMatchers(HttpMethod.POST, "/api/rides").hasRole("PASSENGER")
                // .requestMatchers(HttpMethod.PUT, "/api/rides/*/accept").hasRole("DRIVER")
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
