package org.vros.api.dto.response;

/**
 * Minimal representation of the locally synchronized Keycloak identity.
 * Never contains credentials or token material.
 */
public record UserResponse(
        Long id,
        String keycloakSubject,
        String email,
        String displayName
) {
}
