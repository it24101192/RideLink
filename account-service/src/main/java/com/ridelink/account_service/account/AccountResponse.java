package com.ridelink.account_service.account;

import java.util.UUID;

public record AccountResponse(UUID userId, String username, String email, String role, String status) {
    public AccountResponse(Account account) {
        this(account.getUserId(), account.getUsername(), account.getEmail(), account.getRole(), account.getStatus());
    }
}
