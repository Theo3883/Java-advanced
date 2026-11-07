package org.vros.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Anonymous request to protected endpoint returns 401 Unauthorized ProblemDetail")
    void anonymousRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://vros.example/errors/unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("CUSTOMER role accessing ADMIN endpoint returns 403 Forbidden ProblemDetail")
    void customerRoleAccessingAdminEndpointReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .with(jwt().authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://vros.example/errors/forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ADMIN role accessing /api/v1/customers returns 200 OK")
    void adminRoleAccessingCustomersReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CUSTOMER cannot read another customer's profile (ownership mismatch -> 403)")
    void customerCannotReadForeignProfile() throws Exception {
        mockMvc.perform(get("/api/v1/customers/999999")
                        .with(jwt().jwt(j -> j.subject("customer-subject"))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CUSTOMER cannot search customers (ADMIN-only)")
    void customerCannotSearchCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers/search").param("keyword", "Maria")
                        .with(jwt().authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }
}
