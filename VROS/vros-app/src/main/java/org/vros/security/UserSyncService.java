package org.vros.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.vros.application.service.CustomerService;
import org.vros.application.service.DriverService;
import org.vros.domain.model.Customer;
import org.vros.domain.model.Driver;
import org.vros.domain.model.User;
import org.vros.domain.port.UserRepository;

import java.time.Instant;

/**
 * Idempotently mirrors the authenticated Keycloak subject into the local {@code users}
 * table and lazily links existing customer/driver profiles by their verified email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncService {

    private final UserRepository userRepository;
    private final CustomerService customerService;
    private final DriverService driverService;

    /**
     * Intentionally not wrapped in a single transaction: the insert is committed by the
     * repository adapter, so a concurrent-insert race can be caught and reconciled by
     * reloading the already-persisted subject. Profile linking is idempotent as well.
     */
    public User syncAuthenticatedUser(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("A valid JWT is required to synchronize the user");
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        User user = userRepository.findByKeycloakSubject(subject)
                .orElseGet(() -> createUser(subject, email, name));

        if (Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")) && user.getEmail() != null) {
            linkProfilesByVerifiedEmail(user);
        }
        return user;
    }

    private User createUser(String subject, String email, String name) {
        try {
            Instant now = Instant.now();
            User newUser = User.builder()
                    .keycloakSubject(subject)
                    .email(email != null && !email.isBlank() ? email : subject + "@keycloak.local")
                    .displayName(name != null && !name.isBlank() ? name : defaultDisplayName(subject))
                    .enabled(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Concurrent user synchronization for subject {}. Reloading from database.", subject);
            return userRepository.findByKeycloakSubject(subject)
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to reconcile user synchronization", ex));
        }
    }

    /**
     * Links pre-existing business profiles that share the verified token email and are
     * not yet linked to another identity. Never overwrites an existing link.
     */
    private void linkProfilesByVerifiedEmail(User user) {
        customerService.findByEmail(user.getEmail())
                .filter(customer -> customer.getUser() == null)
                .ifPresent(customer -> {
                    customer.setUser(user);
                    customerService.save(customer);
                    log.info("Linked customer profile {} to Keycloak subject {}",
                            customer.getId(), user.getKeycloakSubject());
                });

        driverService.findByEmail(user.getEmail())
                .filter(driver -> driver.getUser() == null)
                .ifPresent(driver -> {
                    driver.setUser(user);
                    driverService.save(driver);
                    log.info("Linked driver profile {} to Keycloak subject {}",
                            driver.getId(), user.getKeycloakSubject());
                });
    }

    private static String defaultDisplayName(String subject) {
        return "User " + (subject.length() >= 8 ? subject.substring(0, 8) : subject);
    }
}
