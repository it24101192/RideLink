package com.ridelink.account_service.account;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * User lookup used by other services (Ride Management calls GET /api/users/{id}
 * with the caller's bearer token and checks the returned "role").
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AccountRepository accountRepository;

    public UserController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // JWT required (any role)
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        Account account = accountRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(account);
    }

    /**
     * Public view of an account. RIDER is reported as PASSENGER because that is the
     * role name Ride Management and Fare &amp; Payment services check for.
     */
    public record UserResponse(UUID id, String username, String role, String status) {
        static UserResponse from(Account account) {
            String role = "RIDER".equals(account.getRole()) ? "PASSENGER" : account.getRole();
            return new UserResponse(account.getId(), account.getUsername(), role, account.getStatus());
        }
    }
}
