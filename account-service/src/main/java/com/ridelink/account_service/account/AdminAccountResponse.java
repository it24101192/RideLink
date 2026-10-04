package com.ridelink.account_service.account;

import java.util.UUID;

public class AdminAccountResponse {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private String status;

    public AdminAccountResponse(Account account) {
        this.id = account.getUserId();
        this.username = account.getUsername();
        this.email = account.getEmail();
        this.role = AccountService.normalizeRole(account.getRole());
        this.status = account.getStatus();
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

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
