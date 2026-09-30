package com.ridelink.account_service.account;

public class AdminAccountResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;

    public AdminAccountResponse(Account account) {
        this.id = account.getId();
        this.username = account.getUsername();
        this.email = account.getEmail();
        this.role = account.getRole();
        this.status = account.getStatus();
    }

    public Long getId() {
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