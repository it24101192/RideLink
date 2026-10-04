
package com.ridelink.account_service.account;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.UUID;

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

    public AccountResponse register(RegistrationRequest request) {
        return registerAs(request, "PASSENGER");
    }

    public AccountResponse registerDriver(RegistrationRequest request) {
        return registerAs(request, "DRIVER");
    }

    private AccountResponse registerAs(RegistrationRequest request, String role) {

        if (accountRepository.existsByUsername(request.username())) {
            throw new AccountAlreadyExistsException(
                    "Username already exists"
            );
        }

        if (accountRepository.existsByEmail(request.email())) {
            throw new AccountAlreadyExistsException(
                    "Email already exists"
            );
        }

        Account account = new Account();
        account.setUsername(request.username().trim());
        account.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(role);
        account.setStatus("ACTIVE");
        return new AccountResponse(accountRepository.save(account));
    }

    public UserIdentityResponse getUserIdentity(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new AccountNotFoundException("Account not found");
        }
        return new UserIdentityResponse(account.getUserId(), normalizeRole(account.getRole()), account.getStatus());
    }

    static String normalizeRole(String role) {
        return "RIDER".equalsIgnoreCase(role) ? "PASSENGER" : role.toUpperCase(Locale.ROOT);
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
                    account.getUserId(),
                    account.getUsername(),
                    normalizeRole(account.getRole()),
                    account.getStatus(),
                    null
            );
        }

        
        String token = jwtService.generateToken(account);


        return new LoginResponse(
                "Login successful",
                account.getUserId(),
                account.getUsername(),
                normalizeRole(account.getRole()),
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
