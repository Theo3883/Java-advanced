package org.vros.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Positive ownership tests: verifies that the legitimate owner/driver is allowed,
 * that HATEOAS does not leak administrative links, and that role mapping is accurate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnershipSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String customerSubject;
    private String customerEmail;
    private String driverSubject;
    private Long customerId;
    private Long driverId;
    private Long vehicleId;
    private Long deliveryId;

    @BeforeEach
    void seedOwnedAggregate() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        customerSubject = "customer-sub-" + suffix;
        customerEmail = "customer-" + suffix + "@vros.test";
        driverSubject = "driver-sub-" + suffix;

        jdbc.update("""
                INSERT INTO users (keycloak_subject, email, display_name, enabled, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, customerSubject, customerEmail, "Customer " + suffix);
        Long customerUserId = jdbc.queryForObject(
                "SELECT id FROM users WHERE keycloak_subject = ?", Long.class, customerSubject);

        jdbc.update("""
                INSERT INTO users (keycloak_subject, email, display_name, enabled, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, driverSubject, "driver-" + suffix + "@vros.test", "Driver " + suffix);
        Long driverUserId = jdbc.queryForObject(
                "SELECT id FROM users WHERE keycloak_subject = ?", Long.class, driverSubject);

        jdbc.update("""
                INSERT INTO customers (name, email, phone, address, lat, lng, user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "Owned Customer " + suffix, customerEmail, "+40700000000",
                "Test Street", 44.43, 26.10, customerUserId);
        customerId = jdbc.queryForObject(
                "SELECT id FROM customers WHERE user_id = ?", Long.class, customerUserId);

        jdbc.update("""
                INSERT INTO depots (name, lat, lng) VALUES (?, ?, ?)
                """, "Test Depot " + suffix, 44.40, 26.05);
        Long depotId = jdbc.queryForObject(
                "SELECT id FROM depots WHERE name = ?", Long.class, "Test Depot " + suffix);

        jdbc.update("""
                INSERT INTO drivers (name, email, phone, user_id)
                VALUES (?, ?, ?, ?)
                """, "Owned Driver " + suffix, "driver-" + suffix + "@vros.test",
                "+40711111111", driverUserId);
        driverId = jdbc.queryForObject(
                "SELECT id FROM drivers WHERE user_id = ?", Long.class, driverUserId);

        jdbc.update("""
                INSERT INTO vehicles (name, capacity, available, depot_id, driver_id)
                VALUES (?, ?, TRUE, ?, ?)
                """, "Test Van " + suffix, 100, depotId, driverId);
        vehicleId = jdbc.queryForObject(
                "SELECT id FROM vehicles WHERE driver_id = ?", Long.class, driverId);

        jdbc.update("""
                INSERT INTO deliveries (customer_id, quantity, notes, timestamp, assigned_driver_id)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)
                """, customerId, 10, "Owned delivery", driverId);
        deliveryId = jdbc.queryForObject(
                "SELECT id FROM deliveries WHERE assigned_driver_id = ?", Long.class, driverId);
    }

    @Test
    @DisplayName("CUSTOMER can read their own profile and does not receive administrative links")
    void customerCanReadOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", customerId)
                        .with(jwt().jwt(j -> j.subject(customerSubject))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links['all-customers']").doesNotExist());
    }

    @Test
    @DisplayName("ADMIN viewing a customer receives the all-customers discovery link")
    void adminReceivesAdministrativeLink() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", customerId)
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links['all-customers']").exists());
    }

    @Test
    @DisplayName("CUSTOMER can update their own profile")
    void customerCanUpdateOwnProfile() throws Exception {
        mockMvc.perform(put("/api/v1/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Owned Customer",
                                  "email": "%s",
                                  "phone": "+40722222222",
                                  "address": "New Address",
                                  "lat": 44.44,
                                  "lng": 26.11
                                }
                                """.formatted(customerEmail))
                        .with(jwt().jwt(j -> j.subject(customerSubject))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CUSTOMER can list deliveries belonging to them")
    void customerCanListOwnDeliveries() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/customer/{id}", customerId)
                        .with(jwt().jwt(j -> j.subject(customerSubject))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DRIVER can update a delivery assigned to them")
    void driverCanUpdateAssignedDelivery() throws Exception {
        mockMvc.perform(put("/api/v1/deliveries/{id}", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": %d, "assignedDriverId": %d, "quantity": 25}
                                """.formatted(customerId, driverId))
                        .with(jwt().jwt(j -> j.subject(driverSubject))
                                .authorities(() -> "ROLE_DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedDriverId").value(driverId));
    }

    @Test
    @DisplayName("DRIVER can update the vehicle assigned to them")
    void driverCanUpdateOwnVehicle() throws Exception {
        Long depotId = jdbc.queryForObject(
                "SELECT depot_id FROM vehicles WHERE id = ?", Long.class, vehicleId);

        mockMvc.perform(put("/api/v1/vehicles/{id}", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Van",
                                  "capacity": 150,
                                  "available": true,
                                  "depotId": %d,
                                  "driverId": %d
                                }
                                """.formatted(depotId, driverId))
                        .with(jwt().jwt(j -> j.subject(driverSubject))
                                .authorities(() -> "ROLE_DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value(driverId));
    }

    @Test
    @DisplayName("DRIVER cannot update a vehicle assigned to someone else")
    void driverCannotUpdateForeignVehicle() throws Exception {
        mockMvc.perform(put("/api/v1/vehicles/{id}", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hijacked", "capacity": 10, "available": true}
                                """)
                        .with(jwt().jwt(j -> j.subject("another-driver-subject"))
                                .authorities(() -> "ROLE_DRIVER")))
                .andExpect(status().isForbidden());
    }
}
