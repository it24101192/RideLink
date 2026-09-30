
package com.ridelink.account_service.account;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AccountService(
            AccountRepository accountRepository,
            JwtService jwtService) {

        this.accountRepository = accountRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Account register(Account account) {

        if (accountRepository.existsByUsername(account.getUsername())) {
            throw new AccountAlreadyExistsException(
                    "Username already exists"
            );
        }

        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new AccountAlreadyExistsException(
                    "Email already exists"
            );
        }

        account.setPassword(
                passwordEncoder.encode(account.getPassword())
        );

        return accountRepository.save(account);
    }

    public LoginResponse login(LoginRequest request) {

        Account account = accountRepository
                .findByUsername(request.getUsername())
                .orElse(null);

        
        if (account == null) {
            return new LoginResponse(
                    "Invalid username or password",
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

       
        if (!passwordEncoder.matches(
                request.getPassword(),
                account.getPassword())) {

            return new LoginResponse(
                    "Invalid username or password",
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

       
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            return new LoginResponse(
                    "Account is not active",
                    account.getId(),
                    account.getUsername(),
                    account.getRole(),
                    account.getStatus(),
                    null
            );
        }

        
        String token = jwtService.generateToken(account);


        return new LoginResponse(
                "Login successful",
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.getStatus(),
                token
        );
    }

    public AccountProfileResponse getAccountProfile(String username) {

        Account account = accountRepository
                .findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found"
                ));

        return new AccountProfileResponse(account);
    }

    public AccountProfileResponse updateAccountProfile(
            String username,
            UpdateAccountRequest request) {

        Account account = accountRepository
                .findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found"
                ));

        accountRepository.findByEmail(request.getEmail())
                .filter(existingAccount ->
                        !existingAccount.getId().equals(account.getId())
                )
                .ifPresent(existingAccount -> {
                    throw new AccountAlreadyExistsException(
                            "Email already exists"
                    );
                });

        account.setEmail(request.getEmail());

        account.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        Account updatedAccount = accountRepository.save(account);

        return new AccountProfileResponse(updatedAccount);
    }

    public AccountProfileResponse deactivateAccount(String username) {

        Account account = accountRepository
                .findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found"
                ));

        if (!"INACTIVE".equalsIgnoreCase(account.getStatus())) {
            account.setStatus("INACTIVE");
            accountRepository.save(account);
        }

        return new AccountProfileResponse(account);
    }

   public java.util.List<AdminAccountResponse> getAllAccounts() {

    return accountRepository.findAll()
            .stream()
            .map(AdminAccountResponse::new)
            .toList();
}

    public void deleteAccount(String username) {

        Account account = accountRepository
                .findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found"
                ));

        accountRepository.delete(account);
    }
}
