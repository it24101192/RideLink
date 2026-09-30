package com.ridelink.account_service.account;

public class AccountProfileResponse {

    private final String username;
    private final String email;
    private final String role;
    private final String status;

    public AccountProfileResponse(Account account) {
        this.username = account.getUsername();
        this.email = account.getEmail();
        this.role = account.getRole();
        this.status = account.getStatus();
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
