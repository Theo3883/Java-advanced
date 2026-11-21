package org.vros.domain.model;

import lombok.*;

import java.time.Instant;

/**
 * Local projection of an identity owned and authenticated by Keycloak.
 * VROS stores only the stable subject/email/display name - never credentials.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String keycloakSubject;
    private String email;
    private String displayName;
    @Builder.Default
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;
}
