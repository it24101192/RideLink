package com.ridelink.account_service;

import com.ridelink.account_service.account.AccountController;
import com.ridelink.account_service.account.AccountAlreadyExistsException;
import com.ridelink.account_service.account.Account;
import com.ridelink.account_service.account.AccountRepository;
import com.ridelink.account_service.account.AccountService;
import com.ridelink.account_service.account.JwtService;
import com.ridelink.account_service.account.SecurityConfig;
import com.ridelink.account_service.account.RegistrationRequest;
import com.ridelink.account_service.account.AccountResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountServiceApplicationTests {

    private static final String TEST_JWT_SECRET =
            "test-jwt-secret-must-be-at-least-32-characters";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void testPublicEndpoint() throws Exception {

        mockMvc.perform(
                get("/api/accounts/test")
        )
        .andExpect(status().isOk())
        .andExpect(
                content().string("Account Service is working!")
        );
    }

    @Test
    void profileWithoutTokenShouldReturn401() throws Exception {

        mockMvc.perform(
                get("/api/accounts/profile")
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void adminWithoutTokenShouldReturn401() throws Exception {

        mockMvc.perform(
                get("/api/accounts/admin/test")
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void riderShouldNotAccessAdminEndpoint() throws Exception {

        when(jwtService.isTokenValid("fake-rider-token"))
                .thenReturn(true);

        when(jwtService.extractUsername("fake-rider-token"))
                .thenReturn("danusika");

        when(jwtService.extractRole("fake-rider-token"))
                .thenReturn("RIDER");

        mockMvc.perform(
                get("/api/accounts/admin/test")
                        .header(
                                "Authorization",
                                "Bearer fake-rider-token"
                        )
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldAccessAdminEndpoint() throws Exception {

        when(jwtService.isTokenValid("fake-admin-token"))
                .thenReturn(true);

        when(jwtService.extractUsername("fake-admin-token"))
                .thenReturn("admin");

        when(jwtService.extractRole("fake-admin-token"))
                .thenReturn("ADMIN");

        mockMvc.perform(
                get("/api/accounts/admin/test")
                        .header(
                                "Authorization",
                                "Bearer fake-admin-token"
                        )
        )
        .andExpect(status().isOk())
        .andExpect(
                content().string("ADMIN access granted!")
        );
    }

    @Test
    void duplicateUsernameShouldReturn409WithClearError() throws Exception {

        when(accountService.register(any()))
                .thenThrow(new AccountAlreadyExistsException(
                        "Username already exists"
                ));

        mockMvc.perform(
                post("/api/accounts/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "rider1",
                                  "email": "rider1@example.com",
                                  "password": "password123",
                                  "role": "RIDER",
                                  "status": "ACTIVE"
                                }
                                """)
        )
        .andExpect(status().isConflict())
        .andExpect(content().json("""
                {
                  "status": 409,
                  "error": "Conflict",
                  "message": "Username already exists"
                }
                """));
    }

    @Test
    void duplicateEmailShouldReturn409WithClearError() throws Exception {

        when(accountService.register(any()))
                .thenThrow(new AccountAlreadyExistsException(
                        "Email already exists"
                ));

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider2",
                          "email": "rider@example.com",
                          "password": "password123",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isConflict())
        .andExpect(content().json("""
                {
                  "status": 409,
                  "error": "Conflict",
                  "message": "Email already exists"
                }
                """));
    }

    @Test
    void blankUsernameShouldReturn400WithValidationError() throws Exception {

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "   ",
                          "email": "rider@example.com",
                          "password": "password123",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors.username")
                .value("Username must not be blank"))
        .andExpect(content().string(not(containsString(
                "password123"
        ))));
    }

    @Test
    void shortUsernameShouldReturn400WithValidationError() throws Exception {

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "ab",
                          "email": "rider@example.com",
                          "password": "password123",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.username")
                .value("Username must be at least 3 characters long"));
    }

    @Test
    void invalidEmailShouldReturn400WithValidationError() throws Exception {

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "email": "not-an-email",
                          "password": "password123",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email")
                .value("Email must be valid"));
    }

    @Test
    void blankPasswordShouldReturn400WithValidationError()
            throws Exception {

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "email": "rider@example.com",
                          "password": "   ",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password")
                .value("Password must not be blank"));
    }

    @Test
    void shortPasswordShouldReturn400WithValidationError()
            throws Exception {

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "email": "rider@example.com",
                          "password": "short",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password")
                .value("Password must be at least 8 characters long"))
        .andExpect(content().string(not(containsString("short"))));
    }

    @Test
    void callerCannotSelectAnElevatedRole() throws Exception {
        when(accountService.register(any())).thenReturn(new AccountResponse(
                java.util.UUID.randomUUID(), "rider1", "rider@example.com", "PASSENGER", "ACTIVE"));

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "email": "rider@example.com",
                          "password": "password123",
                          "role": "ADMIN",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("PASSENGER"));
    }

    @Test
    void driverProvisioningRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/accounts/admin/drivers")
                .contentType("application/json")
                .content("""
                        {"username":"driver1","email":"driver@example.com","password":"password123"}
                        """))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationDoesNotRequireRoleOrStatus() throws Exception {
        when(accountService.register(any())).thenReturn(new AccountResponse(
                java.util.UUID.randomUUID(), "rider1", "rider@example.com", "PASSENGER", "ACTIVE"));

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "email": "rider@example.com",
                          "password": "password123",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("PASSENGER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void registrationIgnoresCallerSuppliedStatus()
            throws Exception {

        when(accountService.register(any())).thenReturn(new AccountResponse(
                java.util.UUID.randomUUID(), "rider1", "rider@example.com", "PASSENGER", "ACTIVE"));

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "email": "rider@example.com",
                          "password": "password123",
                          "role": "RIDER"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.role").value("PASSENGER"));
    }

    @Test
    void validRegistrationShouldCallAccountService() throws Exception {

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "email": "rider@example.com",
                          "password": "password123",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isOk());

        ArgumentCaptor<RegistrationRequest> accountCaptor =
                ArgumentCaptor.forClass(RegistrationRequest.class);

        verify(accountService).register(accountCaptor.capture());

        RegistrationRequest registration = accountCaptor.getValue();
        assertEquals("rider1", registration.username());
        assertEquals("rider@example.com", registration.email());
        assertEquals("password123", registration.password());
    }

    @Test
    void multipleValidationErrorsShouldReturnAllFieldErrors()
            throws Exception {

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": " ",
                          "email": "not-an-email",
                          "password": "",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.username").exists())
        .andExpect(jsonPath("$.errors.email")
                .value("Email must be valid"))
        .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void unexpectedExceptionShouldReturnSafe500Response()
            throws Exception {

        when(accountService.register(any()))
                .thenThrow(new RuntimeException(
                        "Database failure: password=secret-password"
                ));

        mockMvc.perform(post("/api/accounts/register")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "email": "rider@example.com",
                          "password": "password123",
                          "role": "RIDER",
                          "status": "ACTIVE"
                        }
                        """))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("""
                {
                  "status": 500,
                  "error": "Internal Server Error",
                  "message": "An unexpected error occurred"
                }
                """))
        .andExpect(content().string(not(containsString(
                "secret-password"
        ))))
        .andExpect(content().string(not(containsString(
                "Database failure"
        ))))
        .andExpect(content().string(not(containsString(
                "RuntimeException"
        ))));
    }

    @Test
    void successfulLoginShouldReturnJwtAndAuthenticateProfile()
            throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        JwtService realJwtService = new JwtService(TEST_JWT_SECRET);
        AccountService realAccountService = new AccountService(
                accountRepository,
                realJwtService
        );
        Account account = createActiveAccount("password123", "RIDER");

        when(accountRepository.findByUsername("rider1"))
                .thenReturn(Optional.of(account));
        when(accountService.login(any()))
                .thenAnswer(invocation -> realAccountService.login(
                        invocation.getArgument(0)
                ));

        MvcResult loginResult = mockMvc.perform(post("/api/accounts/login")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "password": "password123"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Login successful"))
        .andExpect(jsonPath("$.username").value("rider1"))
        .andExpect(jsonPath("$.role").value("PASSENGER"))
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(content().string(not(containsString(
                "password123"
        ))))
        .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(
                loginResult.getResponse().getContentAsString(),
                "$.token"
        );

        when(jwtService.isTokenValid(token))
                .thenReturn(realJwtService.isTokenValid(token));
        when(jwtService.extractUsername(token))
                .thenReturn(realJwtService.extractUsername(token));
        when(jwtService.extractRole(token))
                .thenReturn(realJwtService.extractRole(token));
        when(accountService.getAccountProfile("rider1"))
                .thenAnswer(invocation -> realAccountService.getAccountProfile(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("rider1"))
        .andExpect(jsonPath("$.email").value("rider1@example.com"))
        .andExpect(jsonPath("$.role").value("PASSENGER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(content().string(not(containsString(
                account.getPassword()
        ))));
    }

    @Test
    void loginWithUnknownUsernameShouldReturnExistingErrorResponse()
            throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = new AccountService(
                accountRepository,
                mock(JwtService.class)
        );

        when(accountRepository.findByUsername("unknown-user"))
                .thenReturn(Optional.empty());
        when(accountService.login(any()))
                .thenAnswer(invocation -> realAccountService.login(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(post("/api/accounts/login")
                .contentType("application/json")
                .content("""
                        {
                          "username": "unknown-user",
                          "password": "password123"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message")
                .value("Invalid username or password"))
        .andExpect(jsonPath("$.token").doesNotExist())
        .andExpect(content().string(not(containsString(
                "password123"
        ))));
    }

    @Test
    void loginWithIncorrectPasswordShouldReturnExistingErrorResponse()
            throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = new AccountService(
                accountRepository,
                mock(JwtService.class)
        );
        Account account = createActiveAccount("correct-password", "RIDER");

        when(accountRepository.findByUsername("rider1"))
                .thenReturn(Optional.of(account));
        when(accountService.login(any()))
                .thenAnswer(invocation -> realAccountService.login(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(post("/api/accounts/login")
                .contentType("application/json")
                .content("""
                        {
                          "username": "rider1",
                          "password": "incorrect-password"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message")
                .value("Invalid username or password"))
        .andExpect(jsonPath("$.token").doesNotExist())
        .andExpect(content().string(not(containsString(
                "incorrect-password"
        ))));
    }

    @Test
    void generatedJwtShouldContainExpectedIdentityClaims() {

        JwtService realJwtService = new JwtService(TEST_JWT_SECRET);
        Account account = createActiveAccount("password123", "DRIVER");
        account.setStatus("ACTIVE");

        String token = realJwtService.generateToken(account);
        SecretKey key = Keys.hmacShaKeyFor(
                TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)
        );
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("rider1", claims.getSubject());
        assertEquals("DRIVER", claims.get("role", String.class));
        assertEquals(account.getUserId().toString(), claims.get("userId", String.class));
        assertEquals("ACTIVE", claims.get("status", String.class));
        assertEquals("rider1", realJwtService.extractUsername(token));
        assertEquals("DRIVER", realJwtService.extractRole(token));
    }

    @Test
    void malformedJwtShouldReturn401WithoutParsingDetails() throws Exception {

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Bearer not-a-valid-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string(not(containsString(
                "MalformedJwtException"
        ))))
        .andExpect(content().string(not(containsString(
                "JWT parsing"
        ))));
    }

    @Test
    void expiredJwtShouldReturn401WithoutExpirationDetails() throws Exception {

        JwtService realJwtService = new JwtService(TEST_JWT_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(
                TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)
        );
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .subject("rider1")
                .claim("role", "RIDER")
                .claim("status", "ACTIVE")
                .issuedAt(new Date(now.getTime() - 2_000))
                .expiration(new Date(now.getTime() - 1_000))
                .signWith(key)
                .compact();

        when(jwtService.isTokenValid(expiredToken))
                .thenAnswer(invocation -> realJwtService.isTokenValid(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string(not(containsString(
                "ExpiredJwtException"
        ))))
        .andExpect(content().string(not(containsString(
                "expired"
        ))));
    }

    @Test
    void tamperedJwtShouldReturn401WithoutSecurityDetails() throws Exception {

        JwtService realJwtService = new JwtService(TEST_JWT_SECRET);
        String validToken = realJwtService.generateToken(
                createActiveAccount("password123", "RIDER")
        );
        int signatureStart = validToken.lastIndexOf('.') + 1;
        char firstSignatureCharacter = validToken.charAt(signatureStart);
        char replacementCharacter = firstSignatureCharacter == 'a' ? 'b' : 'a';
        String tamperedToken = validToken.substring(0, signatureStart)
                + replacementCharacter
                + validToken.substring(signatureStart + 1);

        when(jwtService.isTokenValid(tamperedToken))
                .thenAnswer(invocation -> realJwtService.isTokenValid(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Bearer " + tamperedToken))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string(not(containsString(
                "SignatureException"
        ))))
        .andExpect(content().string(not(containsString(
                "SecurityException"
        ))));
    }

    @Test
    void wrongSecretJwtShouldReturn401() throws Exception {

        JwtService wrongSecretJwtService = new JwtService(
                "different-test-jwt-secret-at-least-32-characters"
        );
        String wrongSecretToken = wrongSecretJwtService.generateToken(
                createActiveAccount("password123", "RIDER")
        );
        JwtService realJwtService = new JwtService(TEST_JWT_SECRET);

        when(jwtService.isTokenValid(wrongSecretToken))
                .thenAnswer(invocation -> realJwtService.isTokenValid(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Bearer " + wrongSecretToken))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string(not(containsString(
                "SignatureException"
        ))));
    }

    @Test
    void emptyBearerTokenShouldReturn401() throws Exception {

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Bearer"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidAuthorizationHeaderShouldReturn401() throws Exception {

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Token not-a-jwt"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserNotFoundShouldReturn404() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = new AccountService(
                accountRepository,
                mock(JwtService.class)
        );

        when(jwtService.isTokenValid("missing-account-token"))
                .thenReturn(true);
        when(jwtService.extractUsername("missing-account-token"))
                .thenReturn("missing-user");
        when(jwtService.extractRole("missing-account-token"))
                .thenReturn("RIDER");
        when(accountRepository.findByUsername("missing-user"))
                .thenReturn(Optional.empty());
        when(accountService.getAccountProfile("missing-user"))
                .thenAnswer(invocation -> realAccountService.getAccountProfile(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Bearer missing-account-token"))
        .andExpect(status().isNotFound())
        .andExpect(content().json("""
                {
                  "status": 404,
                  "error": "Not Found",
                  "message": "Account not found"
                }
                """))
        .andExpect(content().string(not(containsString(
                "AccountNotFoundException"
        ))));

        verify(accountRepository).findByUsername("missing-user");
    }

    @Test
    void authenticatedUserShouldUpdateEmailAndPassword() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = createAccountService(
                accountRepository
        );
        Account account = createActiveAccount("old-password", "RIDER");
        String originalPasswordHash = account.getPassword();

        authenticateUser("update-token", "rider1", "RIDER");
        when(accountRepository.findByUsername("rider1"))
                .thenReturn(Optional.of(account));
        when(accountRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountService.updateAccountProfile(any(), any()))
                .thenAnswer(invocation -> realAccountService
                        .updateAccountProfile(
                                invocation.getArgument(0),
                                invocation.getArgument(1)
                        ));

        mockMvc.perform(put("/api/accounts/profile")
                .header("Authorization", "Bearer update-token")
                .contentType("application/json")
                .content("""
                        {
                          "email": "new@example.com",
                          "password": "new-password123"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("rider1"))
        .andExpect(jsonPath("$.email").value("new@example.com"))
        .andExpect(jsonPath("$.role").value("PASSENGER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(content().string(not(containsString(
                "new-password123"
        ))))
        .andExpect(content().string(not(containsString(
                originalPasswordHash
        ))));

        assertEquals("new@example.com", account.getEmail());
        assertNotEquals("new-password123", account.getPassword());
        assertNotEquals(originalPasswordHash, account.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches(
                "new-password123",
                account.getPassword()
        ));
        verify(accountRepository).save(account);
    }

    @Test
    void profileUpdateShouldNotChangeUsernameRoleOrStatus() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = createAccountService(
                accountRepository
        );
        Account account = createActiveAccount("old-password", "RIDER");

        authenticateUser("restricted-fields-token", "rider1", "RIDER");
        when(accountRepository.findByUsername("rider1"))
                .thenReturn(Optional.of(account));
        when(accountRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountService.updateAccountProfile(any(), any()))
                .thenAnswer(invocation -> realAccountService
                        .updateAccountProfile(
                                invocation.getArgument(0),
                                invocation.getArgument(1)
                        ));

        mockMvc.perform(put("/api/accounts/profile")
                .header("Authorization", "Bearer restricted-fields-token")
                .contentType("application/json")
                .content("""
                        {
                          "username": "another-user",
                          "email": "new@example.com",
                          "password": "new-password123",
                          "role": "ADMIN",
                          "status": "INACTIVE"
                        }
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("rider1"))
        .andExpect(jsonPath("$.role").value("PASSENGER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertEquals("rider1", account.getUsername());
        assertEquals("RIDER", account.getRole());
        assertEquals("ACTIVE", account.getStatus());
    }

    @Test
    void invalidProfileUpdateEmailShouldReturn400() throws Exception {

        authenticateUser("invalid-email-token", "rider1", "RIDER");

        mockMvc.perform(put("/api/accounts/profile")
                .header("Authorization", "Bearer invalid-email-token")
                .contentType("application/json")
                .content("""
                        {
                          "email": "not-an-email",
                          "password": "new-password123"
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email")
                .value("Email must be valid"));
    }

    @Test
    void blankProfileUpdatePasswordShouldReturn400() throws Exception {

        authenticateUser("blank-password-token", "rider1", "RIDER");

        mockMvc.perform(put("/api/accounts/profile")
                .header("Authorization", "Bearer blank-password-token")
                .contentType("application/json")
                .content("""
                        {
                          "email": "new@example.com",
                          "password": "   "
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password")
                .value("Password must not be blank"));
    }

    @Test
    void shortProfileUpdatePasswordShouldReturn400() throws Exception {

        authenticateUser("short-password-token", "rider1", "RIDER");

        mockMvc.perform(put("/api/accounts/profile")
                .header("Authorization", "Bearer short-password-token")
                .contentType("application/json")
                .content("""
                        {
                          "email": "new@example.com",
                          "password": "short"
                        }
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password")
                .value("Password must be at least 8 characters long"))
        .andExpect(content().string(not(containsString("short"))));
    }

    @Test
    void duplicateProfileUpdateEmailShouldReturn409() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = createAccountService(
                accountRepository
        );
        Account account = createActiveAccount("old-password", "RIDER");
        Account anotherAccount = createActiveAccount("other-password", "DRIVER");
        anotherAccount.setId(2L);

        authenticateUser("duplicate-email-token", "rider1", "RIDER");
        when(accountRepository.findByUsername("rider1"))
                .thenReturn(Optional.of(account));
        when(accountRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(anotherAccount));
        when(accountService.updateAccountProfile(any(), any()))
                .thenAnswer(invocation -> realAccountService
                        .updateAccountProfile(
                                invocation.getArgument(0),
                                invocation.getArgument(1)
                        ));

        mockMvc.perform(put("/api/accounts/profile")
                .header("Authorization", "Bearer duplicate-email-token")
                .contentType("application/json")
                .content("""
                        {
                          "email": "taken@example.com",
                          "password": "new-password123"
                        }
                        """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void profileUpdateWithoutJwtShouldReturn401() throws Exception {

        mockMvc.perform(put("/api/accounts/profile")
                .contentType("application/json")
                .content("""
                        {
                          "email": "new@example.com",
                          "password": "new-password123"
                        }
                        """))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtShouldRejectProfileUpdateWith401() throws Exception {

        mockMvc.perform(put("/api/accounts/profile")
                .header("Authorization", "Bearer not-a-valid-jwt")
                .contentType("application/json")
                .content("""
                        {
                          "email": "new@example.com",
                          "password": "new-password123"
                        }
                        """))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void missingAccountProfileUpdateShouldReturn404() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = createAccountService(
                accountRepository
        );

        authenticateUser("missing-update-account-token", "missing-user", "RIDER");
        when(accountRepository.findByUsername("missing-user"))
                .thenReturn(Optional.empty());
        when(accountService.updateAccountProfile(any(), any()))
                .thenAnswer(invocation -> realAccountService
                        .updateAccountProfile(
                                invocation.getArgument(0),
                                invocation.getArgument(1)
                        ));

        mockMvc.perform(put("/api/accounts/profile")
                .header("Authorization", "Bearer missing-update-account-token")
                .contentType("application/json")
                .content("""
                        {
                          "email": "new@example.com",
                          "password": "new-password123"
                        }
                        """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void authenticatedUserShouldDeactivateOwnAccount() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = createAccountService(
                accountRepository
        );
        Account account = createActiveAccount("password123", "RIDER");
        String originalEmail = account.getEmail();
        String originalPassword = account.getPassword();

        authenticateUser("deactivate-token", "rider1", "RIDER");
        when(accountRepository.findByUsername("rider1"))
                .thenReturn(Optional.of(account));
        when(accountRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountService.deactivateAccount("rider1"))
                .thenAnswer(invocation -> realAccountService.deactivateAccount(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(patch("/api/accounts/profile/deactivate")
                .header("Authorization", "Bearer deactivate-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("rider1"))
        .andExpect(jsonPath("$.email").value(originalEmail))
        .andExpect(jsonPath("$.role").value("PASSENGER"))
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(content().string(not(containsString(
                originalPassword
        ))));

        assertEquals("INACTIVE", account.getStatus());
        assertEquals("rider1", account.getUsername());
        assertEquals(originalEmail, account.getEmail());
        assertEquals(originalPassword, account.getPassword());
        assertEquals("RIDER", account.getRole());
        verify(accountRepository).save(account);
        verify(accountRepository, never()).delete(any());
    }

    @Test
    void alreadyInactiveAccountShouldNotChangeOtherData() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = createAccountService(
                accountRepository
        );
        Account account = createActiveAccount("password123", "RIDER");
        account.setStatus("INACTIVE");
        String originalEmail = account.getEmail();
        String originalPassword = account.getPassword();

        authenticateUser("already-inactive-token", "rider1", "RIDER");
        when(accountRepository.findByUsername("rider1"))
                .thenReturn(Optional.of(account));
        when(accountService.deactivateAccount("rider1"))
                .thenAnswer(invocation -> realAccountService.deactivateAccount(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(patch("/api/accounts/profile/deactivate")
                .header("Authorization", "Bearer already-inactive-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));

        assertEquals("rider1", account.getUsername());
        assertEquals(originalEmail, account.getEmail());
        assertEquals(originalPassword, account.getPassword());
        assertEquals("RIDER", account.getRole());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void missingJwtShouldRejectDeactivationWith401() throws Exception {

        mockMvc.perform(patch("/api/accounts/profile/deactivate"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJwtShouldRejectDeactivationWith401() throws Exception {

        mockMvc.perform(patch("/api/accounts/profile/deactivate")
                .header("Authorization", "Bearer not-a-valid-jwt"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredJwtShouldRejectDeactivationWith401() throws Exception {

        JwtService realJwtService = new JwtService(TEST_JWT_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(
                TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)
        );
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .subject("rider1")
                .claim("role", "RIDER")
                .claim("status", "ACTIVE")
                .issuedAt(new Date(now.getTime() - 2_000))
                .expiration(new Date(now.getTime() - 1_000))
                .signWith(key)
                .compact();

        when(jwtService.isTokenValid(expiredToken))
                .thenAnswer(invocation -> realJwtService.isTokenValid(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(patch("/api/accounts/profile/deactivate")
                .header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string(not(containsString(
                "ExpiredJwtException"
        ))));
    }

    @Test
    void missingAccountDeactivationShouldReturn404() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService realAccountService = createAccountService(
                accountRepository
        );

        authenticateUser("missing-deactivate-token", "missing-user", "RIDER");
        when(accountRepository.findByUsername("missing-user"))
                .thenReturn(Optional.empty());
        when(accountService.deactivateAccount("missing-user"))
                .thenAnswer(invocation -> realAccountService.deactivateAccount(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(patch("/api/accounts/profile/deactivate")
                .header("Authorization", "Bearer missing-deactivate-token"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void inactiveAccountCanStillUseExistingValidJwt() throws Exception {

        AccountRepository accountRepository = mock(AccountRepository.class);
        JwtService realJwtService = new JwtService(TEST_JWT_SECRET);
        AccountService realAccountService = new AccountService(
                accountRepository,
                realJwtService
        );
        Account account = createActiveAccount("password123", "RIDER");
        String token = realJwtService.generateToken(account);
        account.setStatus("INACTIVE");

        when(accountRepository.findByUsername("rider1"))
                .thenReturn(Optional.of(account));
        when(jwtService.isTokenValid(token))
                .thenReturn(realJwtService.isTokenValid(token));
        when(jwtService.extractUsername(token))
                .thenReturn(realJwtService.extractUsername(token));
        when(jwtService.extractRole(token))
                .thenReturn(realJwtService.extractRole(token));
        when(accountService.getAccountProfile("rider1"))
                .thenAnswer(invocation -> realAccountService.getAccountProfile(
                        invocation.getArgument(0)
                ));

        mockMvc.perform(get("/api/accounts/profile")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    private Account createActiveAccount(String password, String role) {

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        Account account = new Account();
        account.setId(1L);
        account.setUserId(java.util.UUID.randomUUID());
        account.setUsername("rider1");
        account.setEmail("rider1@example.com");
        account.setPassword(passwordEncoder.encode(password));
        account.setRole(role);
        account.setStatus("ACTIVE");

        return account;
    }

    private AccountService createAccountService(
            AccountRepository accountRepository) {

        return new AccountService(accountRepository, mock(JwtService.class));
    }

    private void authenticateUser(
            String token,
            String username,
            String role) {

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.extractRole(token)).thenReturn(role);
    }
}
