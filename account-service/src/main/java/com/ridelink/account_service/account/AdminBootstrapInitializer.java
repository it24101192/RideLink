package com.ridelink.account_service.account;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first administrator only when explicit deployment credentials are configured. */
@Component
public class AdminBootstrapInitializer implements CommandLineRunner {
    private final AccountRepository accounts;
    private final String username;
    private final String email;
    private final String password;

    public AdminBootstrapInitializer(AccountRepository accounts,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.accounts = accounts;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (username.isBlank() && email.isBlank() && password.isBlank()) return;
        if (username.isBlank() || email.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Configure ADMIN_BOOTSTRAP_USERNAME, ADMIN_BOOTSTRAP_EMAIL, and a password of at least 12 characters together");
        }
        var existing = accounts.findByUsername(username.trim());
        if (existing.isPresent()) {
            if (!"ADMIN".equalsIgnoreCase(existing.get().getRole())) {
                throw new IllegalStateException("Configured bootstrap username already belongs to a non-admin account");
            }
            return;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Configured bootstrap email already belongs to another account");
        }
        Account admin = new Account();
        admin.setUsername(username.trim());
        admin.setEmail(normalizedEmail);
        admin.setPassword(new BCryptPasswordEncoder().encode(password));
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        accounts.save(admin);
    }
}
