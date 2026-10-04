package com.ridelink.account_service.account;

import java.util.UUID;

public class AccountProfileResponse {

    private final String username;
    private final UUID userId;
    private final String email;
    private final String role;
    private final String status;

    public AccountProfileResponse(Account account) {
        this.username = account.getUsername();
        this.userId = account.getUserId();
        this.email = account.getEmail();
        this.role = AccountService.normalizeRole(account.getRole());
        this.status = account.getStatus();
    }

    public String getUsername() {
        return username;
    }

    public UUID getUserId() { return userId; }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }
}
