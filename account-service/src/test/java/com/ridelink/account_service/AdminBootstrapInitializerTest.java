package com.ridelink.account_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ridelink.account_service.account.Account;
import com.ridelink.account_service.account.AccountRepository;
import com.ridelink.account_service.account.AdminBootstrapInitializer;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminBootstrapInitializerTest {
    @Test
    void skipsBootstrapWhenCredentialsAreNotConfigured() {
        AccountRepository accounts = mock(AccountRepository.class);

        new AdminBootstrapInitializer(accounts, "", "", "").run();

        verify(accounts, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsAdminWithHashedPasswordWhenCredentialsAreConfigured() {
        AccountRepository accounts = mock(AccountRepository.class);
        when(accounts.findByUsername("local-admin")).thenReturn(Optional.empty());
        when(accounts.existsByEmail("admin@localhost")).thenReturn(false);
        AdminBootstrapInitializer initializer = new AdminBootstrapInitializer(accounts,
                "local-admin", "Admin@LocalHost", "a-private-password-123");

        initializer.run();

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(saved.capture());
        assertEquals("ADMIN", saved.getValue().getRole());
        assertEquals("ACTIVE", saved.getValue().getStatus());
        assertEquals("admin@localhost", saved.getValue().getEmail());
        assertTrue(new BCryptPasswordEncoder().matches("a-private-password-123", saved.getValue().getPassword()));
    }

    @Test
    void doesNotPromoteAnExistingNonAdminAccount() {
        AccountRepository accounts = mock(AccountRepository.class);
        Account existing = new Account();
        existing.setRole("PASSENGER");
        when(accounts.findByUsername("local-admin")).thenReturn(Optional.of(existing));
        AdminBootstrapInitializer initializer = new AdminBootstrapInitializer(accounts,
                "local-admin", "admin@localhost", "a-private-password-123");

        assertThrows(IllegalStateException.class, initializer::run);
        verify(accounts, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
