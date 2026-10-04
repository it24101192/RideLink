
package com.ridelink.account_service.account;

import java.util.UUID;

public class LoginResponse {

    private String message;
    private UUID accountId;
    private String username;
    private String role;
    private String status;
    private String token;

    public LoginResponse() {
    }

    public LoginResponse(
            String message,
            UUID accountId,
            String username,
            String role,
            String status,
            String token) {

        this.message = message;
        this.accountId = accountId;
        this.username = username;
        this.role = role;
        this.status = status;
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }
}
