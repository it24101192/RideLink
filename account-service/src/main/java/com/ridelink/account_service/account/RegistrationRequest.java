package com.ridelink.account_service.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistrationRequest(
        @NotBlank(message = "Username must not be blank")
        @Size(min = 3, message = "Username must be at least 3 characters long")
        @Size(max = 100, message = "Username must not exceed 100 characters") String username,
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must not exceed 254 characters") String email,
        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        @Size(max = 72, message = "Password must not exceed 72 characters") String password) { }
