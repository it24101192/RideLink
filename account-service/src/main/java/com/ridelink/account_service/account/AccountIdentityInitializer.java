package com.ridelink.account_service.account;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountIdentityInitializer implements CommandLineRunner {
    private final AccountRepository accounts;

    public AccountIdentityInitializer(AccountRepository accounts) { this.accounts = accounts; }

    @Override
    @Transactional
    public void run(String... args) {
        var legacyAccounts = accounts.findAll().stream()
                .filter(account -> account.getUserId() == null)
                .toList();
        legacyAccounts.forEach(Account::assignUserId);
        accounts.saveAll(legacyAccounts);
    }
}
