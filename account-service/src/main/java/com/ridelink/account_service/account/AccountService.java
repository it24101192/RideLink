package com.ridelink.account_service.account;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AccountService(
            AccountRepository accountRepository,
            JwtService jwtService) {

        this.accountRepository = accountRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /** Roles understood by Ride Management and Fare &amp; Payment services. RIDER is treated as PASSENGER there. */
    static final Set<String> ALLOWED_ROLES = Set.of("PASSENGER", "RIDER", "DRIVER", "ADMIN");

    public Account register(Account account) {

        String role = account.getRole() == null ? "" : account.getRole().trim().toUpperCase(Locale.ROOT);
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must be one of PASSENGER, RIDER, DRIVER, ADMIN");
        }
        account.setRole(role);
        account.setStatus(account.getStatus().trim().toUpperCase(Locale.ROOT));

        if (accountRepository.existsByUsername(account.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        account.setPassword(
                passwordEncoder.encode(account.getPassword())
        );

        return accountRepository.save(account);
    }

    public LoginResponse login(LoginRequest request) {

        Account account = accountRepository
                .findByUsername(request.getUsername())
                .orElse(null);

        // Username not found
        if (account == null) {
            return new LoginResponse(
                    "Invalid username or password",
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        // Password incorrect
        if (!passwordEncoder.matches(
                request.getPassword(),
                account.getPassword())) {

            return new LoginResponse(
                    "Invalid username or password",
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        // Account not active
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            return new LoginResponse(
                    "Account is not active",
                    account.getId(),
                    account.getUsername(),
                    account.getRole(),
                    account.getStatus(),
                    null
            );
        }

        // Generate JWT token
        String token = jwtService.generateToken(account);

        // Successful login
        return new LoginResponse(
                "Login successful",
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.getStatus(),
                token
        );
    }
}