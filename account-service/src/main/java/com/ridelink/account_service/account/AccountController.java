
package com.ridelink.account_service.account;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
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

    @GetMapping("/test")
    public String test() {
        return "Account Service is working!";
    }

    
    @PostMapping("/register")
    public Account register(@Valid @RequestBody Account account) {
        return accountService.register(account);
    }

    
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return accountService.login(request);
    }

    
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile")
    public AccountProfileResponse profile(
            Authentication authentication) {

        return accountService.getAccountProfile(
                authentication.getName()
        );
    }

    
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

   
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/profile/deactivate")
    public AccountProfileResponse deactivateProfile(
            Authentication authentication) {

        return accountService.deactivateAccount(
                authentication.getName()
        );
    }

    
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/profile")
    public String deleteProfile(
            Authentication authentication) {

        accountService.deleteAccount(
                authentication.getName()
        );

        return "Account deleted successfully";
    }
    
    @SecurityRequirement(name = "bearerAuth")
@GetMapping("/admin/test")
public String adminTest() {
    return "ADMIN access granted!";
}

        @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/admin/accounts")
    public List<AdminAccountResponse> getAllAccounts() {
        return accountService.getAllAccounts();
    }
}
