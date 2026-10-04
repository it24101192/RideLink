package com.ridelink.account_service.account;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AccountService accounts;

    public UserController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/{userId}")
    public UserIdentityResponse getById(@PathVariable UUID userId) {
        return accounts.getUserIdentity(userId);
    }
}
