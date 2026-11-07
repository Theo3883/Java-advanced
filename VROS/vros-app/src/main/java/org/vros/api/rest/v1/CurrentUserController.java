package org.vros.api.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vros.api.dto.request.CustomerRequest;
import org.vros.api.dto.response.CustomerResponse;
import org.vros.api.dto.response.UserResponse;
import org.vros.api.rest.v1.assembler.CustomerModelAssembler;
import org.vros.application.service.CustomerService;
import org.vros.domain.exception.ConflictException;
import org.vros.domain.model.Coordinates;
import org.vros.domain.model.Customer;
import org.vros.domain.model.User;
import org.vros.security.UserSyncService;

/**
 * Self-service profile access for the authenticated Keycloak identity.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Current authenticated user profile and onboarding")
public class CurrentUserController {

    private final UserSyncService userSyncService;
    private final CustomerService customerService;
    private final CustomerModelAssembler customerModelAssembler;

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user account")
    public UserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        User user = userSyncService.syncAuthenticatedUser(jwt);
        return new UserResponse(user.getId(), user.getKeycloakSubject(),
                user.getEmail(), user.getDisplayName());
    }

    @PostMapping("/me/customer-profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Self-provision (or update) the customer profile of the authenticated user")
    public ResponseEntity<CustomerResponse> createMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CustomerRequest req) {

        User user = userSyncService.syncAuthenticatedUser(jwt);

        Customer customer = customerService.findByEmail(user.getEmail())
                .map(existing -> {
                    if (existing.getUser() != null
                            && !existing.getUser().getId().equals(user.getId())) {
                        throw new ConflictException(user.getEmail());
                    }
                    existing.setUser(user);
                    applyProfile(existing, req);
                    return existing;
                })
                .orElseGet(() -> {
                    Customer created = Customer.builder()
                            .name(req.getName())
                            .email(user.getEmail())
                            .phone(req.getPhone())
                            .address(req.getAddress())
                            .coordinates(new Coordinates(req.getLat(), req.getLng()))
                            .build();
                    created.setUser(user);
                    return created;
                });

        Customer saved = customerService.save(customer);
        return ResponseEntity.ok(customerModelAssembler.toModel(saved));
    }

    private static void applyProfile(Customer customer, CustomerRequest req) {
        customer.setName(req.getName());
        customer.setPhone(req.getPhone());
        customer.setAddress(req.getAddress());
        customer.setCoordinates(new Coordinates(req.getLat(), req.getLng()));
    }
}
