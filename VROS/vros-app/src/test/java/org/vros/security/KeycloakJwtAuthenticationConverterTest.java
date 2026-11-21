package org.vros.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter =
            new KeycloakJwtAuthenticationConverter("vros-api");

    private static Jwt keycloakJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .claim("preferred_username", "admin_user")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "offline_access")))
                .claim("resource_access", Map.of(
                        "vros-api", Map.of("roles", List.of("CUSTOMER", "DRIVER")),
                        "other-client", Map.of("roles", List.of("IGNORED"))))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    void mapsRealmAndClientRolesToPrefixedAuthorities() {
        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) converter.convert(keycloakJwt());

        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder(
                        "ROLE_ADMIN",
                        "ROLE_OFFLINE_ACCESS",
                        "ROLE_CUSTOMER",
                        "ROLE_DRIVER");

        assertThat(authentication.getName()).isEqualTo("admin_user");
    }

    @Test
    void ignoresRolesOfOtherClients() {
        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) converter.convert(keycloakJwt());

        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .doesNotContain("ROLE_IGNORED");
    }
}
