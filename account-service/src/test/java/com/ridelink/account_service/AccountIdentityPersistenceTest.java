package com.ridelink.account_service;

import com.ridelink.account_service.account.Account;
import com.ridelink.account_service.account.AccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class AccountIdentityPersistenceTest {
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private EntityManager entityManager;

    @Test
    void findsPersistedAccountByUuidAfterClearingPersistenceContext() {
        Account account = new Account();
        account.setUsername("identity-roundtrip");
        account.setEmail("identity-roundtrip@localhost");
        account.setPassword("test-password-hash");
        account.setRole("PASSENGER");
        account.setStatus("ACTIVE");
        var saved = accounts.saveAndFlush(account);
        var userId = saved.getUserId();
        entityManager.clear();

        assertEquals(userId, accounts.findByUserId(userId).orElseThrow().getUserId());
        Number storedBytes = (Number) entityManager.createNativeQuery(
                "select octet_length(user_id) from account where id = :id")
                .setParameter("id", saved.getId()).getSingleResult();
        assertEquals(16, storedBytes.intValue());
    }
}
