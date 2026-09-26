package com.ridelink.account_service;

import com.ridelink.account_service.account.AccountController;
import com.ridelink.account_service.account.AccountService;
import com.ridelink.account_service.account.JwtService;
import com.ridelink.account_service.account.SecurityConfig;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountServiceApplicationTests {

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
}