
package com.ridelink.account_service.account;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Public test endpoint
    @GetMapping("/test")
    public String test() {
        return "Account Service is working!";
    }

    // Public registration endpoint
    @PostMapping("/register")
    public Account register(@Valid @RequestBody Account account) {
        return accountService.register(account);
    }

    // Public login endpoint
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return accountService.login(request);
    }

    // JWT required
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile")
    public AccountProfileResponse profile(
            Authentication authentication) {

        return accountService.getAccountProfile(
                authentication.getName()
        );
    }

    // JWT required
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    public AccountProfileResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateAccountRequest request) {

        return accountService.updateAccountProfile(
                authentication.getName(),
                request
        );
    }

    // JWT required
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/profile/deactivate")
    public AccountProfileResponse deactivateProfile(
            Authentication authentication) {

        return accountService.deactivateAccount(
                authentication.getName()
        );
    }

    // JWT required
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/profile")
    public String deleteProfile(
            Authentication authentication) {

        accountService.deleteAccount(
                authentication.getName()
        );

        return "Account deleted successfully";
    }

    // JWT + ADMIN role required
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/admin/test")
    public String adminTest() {
        return "ADMIN access granted!";
    }
}
