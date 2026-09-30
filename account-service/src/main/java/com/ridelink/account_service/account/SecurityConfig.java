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

                
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                .requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**"
                ).permitAll()

                
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/accounts/test"
                ).permitAll()

                
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/accounts/register",
                    "/api/accounts/login"
                ).permitAll()

                
                .requestMatchers(
                "/api/accounts/admin/test",
                "/api/accounts/admin/accounts"
                ).hasAuthority("ROLE_ADMIN")

               
                .requestMatchers(
                    "/api/accounts/driver/**"
                ).hasAuthority("ROLE_DRIVER")

                
                .requestMatchers(
                    "/api/accounts/rider/**"
                ).hasAuthority("ROLE_RIDER")

               
                .anyRequest().authenticated()
            )

            .exceptionHandling(exception ->
                exception
                    
                    .authenticationEntryPoint(
                        (request, response, authException) ->
                            response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Unauthorized"
                            )
                    )

                   
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