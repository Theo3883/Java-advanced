package org.vros.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliverySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Anonymous create delivery returns 401 Unauthorized")
    void anonymousCreateReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "quantity": 100}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CUSTOMER cannot bulk-list deliveries (ADMIN-only)")
    void customerCannotListAllDeliveries() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries")
                        .with(jwt().authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRIVER cannot bulk-list all deliveries (ADMIN-only)")
    void driverCannotListAllDeliveries() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries")
                        .with(jwt().authorities(() -> "ROLE_DRIVER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can bulk-list deliveries")
    void adminCanListAllDeliveries() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CUSTOMER cannot list another customer's deliveries (ownership mismatch -> 403)")
    void customerCannotListForeignDeliveries() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/customer/999999")
                        .with(jwt().jwt(j -> j.subject("customer-subject"))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CUSTOMER cannot read an unrelated delivery")
    void customerCannotReadForeignDelivery() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/999999")
                        .with(jwt().jwt(j -> j.subject("customer-subject"))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DRIVER cannot update a delivery that is not assigned to them")
    void driverCannotUpdateUnassignedDelivery() throws Exception {
        mockMvc.perform(put("/api/v1/deliveries/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "quantity": 10}
                                """)
                        .with(jwt().jwt(j -> j.subject("driver-subject"))
                                .authorities(() -> "ROLE_DRIVER")))
                .andExpect(status().isForbidden());
    }
}
