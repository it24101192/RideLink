package com.ridelink.account_service.account;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .httpBasic(basic -> basic.disable())

            .formLogin(form -> form.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Allow Spring Boot error dispatches
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                .requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**"
                ).permitAll()

                // Public GET endpoint
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/accounts/test"
                ).permitAll()

                // Public POST endpoints
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/accounts/register",
                    "/api/accounts/login"
                ).permitAll()

                // ADMIN only
                .requestMatchers(
                    "/api/accounts/admin/test"
                ).hasAuthority("ROLE_ADMIN")

                // DRIVER only
                .requestMatchers(
                    "/api/accounts/driver/**"
                ).hasAuthority("ROLE_DRIVER")

                // RIDER only
                .requestMatchers(
                    "/api/accounts/rider/**"
                ).hasAuthority("ROLE_RIDER")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            .exceptionHandling(exception ->
                exception
                    // No authentication → 401
                    .authenticationEntryPoint(
                        (request, response, authException) ->
                            response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Unauthorized"
                            )
                    )

                    // Wrong role → 403
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            response.sendError(
                                HttpServletResponse.SC_FORBIDDEN,
                                "Forbidden"
                            )
                    )
            )

            .addFilterBefore(
                new JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}