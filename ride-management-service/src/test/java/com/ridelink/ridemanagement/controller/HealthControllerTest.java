package com.ridelink.ridemanagement.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import com.ridelink.ridemanagement.config.SecurityConfig;

@WebMvcTest(controllers = HealthController.class, properties = "security.jwt.secret=01234567890123456789012345678901")
@Import(SecurityConfig.class)
class HealthControllerTest {
    @Autowired private MockMvc mvc;
    @Test void healthIsPublic() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void rideApiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/rides")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
