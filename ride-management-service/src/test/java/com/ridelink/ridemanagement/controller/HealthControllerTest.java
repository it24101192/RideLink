package com.ridelink.ridemanagement.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import com.ridelink.ridemanagement.config.SecurityConfig;
import com.ridelink.ridemanagement.config.OpenApiResourceConfig;

@WebMvcTest(controllers = HealthController.class, properties = "security.jwt.secret=01234567890123456789012345678901")
@Import({SecurityConfig.class, OpenApiResourceConfig.class})
class HealthControllerTest {
    @Autowired private MockMvc mvc;
    @Test void healthIsPublic() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void rideApiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/rides")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
    @Test void swaggerSpecificationIsPublicAndServedFromClasspath() throws Exception {
        mvc.perform(get("/openapi.yaml")).andExpect(status().isOk())
            .andExpect(content().string(startsWith("openapi: 3.0.3")));
    }
}
