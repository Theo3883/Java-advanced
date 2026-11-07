package org.vros.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.vros.application.service.CustomerService;
import org.vros.application.service.DeliveryService;
import org.vros.application.service.DriverService;
import org.vros.application.service.VehicleService;
import org.vros.domain.model.Customer;
import org.vros.domain.model.Delivery;
import org.vros.domain.model.Driver;
import org.vros.domain.model.User;
import org.vros.domain.model.Vehicle;
import org.vros.domain.port.ScenarioRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Resource-ownership evaluator used from {@code @PreAuthorize} SpEL expressions.
 * Keeps all Spring Security knowledge in the adapter layer while relying solely on
 * domain services and ports.
 */
@Component("ownershipChecker")
@RequiredArgsConstructor
public class OwnershipChecker {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final CustomerService customerService;
    private final DeliveryService deliveryService;
    private final DriverService driverService;
    private final VehicleService vehicleService;
    private final ScenarioRepository scenarioRepository;

    public boolean isCustomerOwner(Authentication authentication, Long customerId) {
        if (isAdmin(authentication)) return true;
        String subject = extractSubject(authentication);
        if (subject == null || customerId == null) return false;

        return customerService.findById(customerId)
                .map(Customer::getUser)
                .filter(Objects::nonNull)
                .map(User::getKeycloakSubject)
                .map(subject::equals)
                .orElse(false);
    }

    public boolean isDeliveryOwner(Authentication authentication, Long deliveryId) {
        if (isAdmin(authentication)) return true;
        String subject = extractSubject(authentication);
        if (subject == null || deliveryId == null) return false;

        Optional<Delivery> deliveryOpt = deliveryService.findById(deliveryId);
        if (deliveryOpt.isEmpty()) return false;

        Customer customer = deliveryOpt.get().getCustomer();
        return customer != null
                && customer.getUser() != null
                && subject.equals(customer.getUser().getKeycloakSubject());
    }

    /**
     * A driver owns a delivery when it is explicitly assigned to them, or (fallback)
     * when the delivery shares a scenario with one of the driver's vehicles.
     */
    public boolean isDeliveryDriver(Authentication authentication, Long deliveryId) {
        if (isAdmin(authentication)) return true;
        String subject = extractSubject(authentication);
        if (subject == null || deliveryId == null) return false;

        Optional<Delivery> deliveryOpt = deliveryService.findById(deliveryId);
        if (deliveryOpt.isPresent()) {
            Driver assigned = deliveryOpt.get().getAssignedDriver();
            if (assigned != null && assigned.getUser() != null
                    && subject.equals(assigned.getUser().getKeycloakSubject())) {
                return true;
            }
        }

        return scenarioRepository.existsByDeliveryIdAndDriverSubject(deliveryId, subject);
    }

    public boolean isVehicleDriver(Authentication authentication, Long vehicleId) {
        if (isAdmin(authentication)) return true;
        String subject = extractSubject(authentication);
        if (subject == null || vehicleId == null) return false;

        return vehicleService.findById(vehicleId)
                .map(Vehicle::getDriver)
                .filter(Objects::nonNull)
                .map(Driver::getUser)
                .filter(Objects::nonNull)
                .map(User::getKeycloakSubject)
                .map(subject::equals)
                .orElse(false);
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }

    public boolean isDriverProfileLinked(Authentication authentication, Long driverId) {
        if (isAdmin(authentication)) return true;
        String subject = extractSubject(authentication);
        if (subject == null || driverId == null) return false;
        return driverService.findById(driverId)
                .map(Driver::getUser)
                .filter(Objects::nonNull)
                .map(User::getKeycloakSubject)
                .map(subject::equals)
                .orElse(false);
    }

    private String extractSubject(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }
}
