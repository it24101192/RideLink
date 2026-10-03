package com.ridelink.account_service;

import com.ridelink.account_service.account.Account;
import com.ridelink.account_service.account.AccountRepository;
import com.ridelink.account_service.account.JwtService;
import com.ridelink.account_service.account.SecurityConfig;
import com.ridelink.account_service.account.UserController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/users/{id} as called by ride-management-service's AccountServiceClient. */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void requiresToken() throws Exception {
        mockMvc.perform(get("/api/users/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsRoleForRideService() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account(id, "DRIVER")));
        authenticate("ADMIN");

        mockMvc.perform(get("/api/users/" + id).header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.role", is("DRIVER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void riderIsReportedAsPassenger() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account(id, "RIDER")));
        authenticate("RIDER");

        mockMvc.perform(get("/api/users/" + id).header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("PASSENGER")));
    }

    @Test
    void unknownUserIs404() throws Exception {
        authenticate("ADMIN");
        mockMvc.perform(get("/api/users/" + UUID.randomUUID()).header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/users/123").header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    private void authenticate(String role) {
        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(jwtService.extractUsername("token")).thenReturn("caller");
        when(jwtService.extractRole("token")).thenReturn(role);
    }

    private static Account account(UUID id, String role) {
        Account account = new Account();
        account.setId(id);
        account.setUsername("user-" + role.toLowerCase());
        account.setPassword("$2a$hash");
        account.setRole(role);
        account.setStatus("ACTIVE");
        return account;
    }
}
