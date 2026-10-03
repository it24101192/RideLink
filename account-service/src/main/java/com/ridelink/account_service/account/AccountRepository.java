package com.ridelink.account_service.account;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);
}