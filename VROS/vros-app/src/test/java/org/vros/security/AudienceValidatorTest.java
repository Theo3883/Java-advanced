package org.vros.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    private static final String ISSUER = "https://keycloak.test.vros.org/realms/vros";

    private final AudienceValidator validator = new AudienceValidator("vros-api");

    private static Jwt jwtWithAudience(List<String> audience) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .audience(audience)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    void acceptsTokenContainingExpectedAudience() {
        assertThat(validator.validate(jwtWithAudience(List.of("vros-api", "account"))).hasErrors())
                .isFalse();
    }

    @Test
    void rejectsExpiredTokenViaDefaultTimestampValidator() {
        Jwt expired = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .audience(List.of("vros-api"))
                .issuedAt(Instant.now().minusSeconds(120))
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        assertThat(JwtValidators.createDefaultWithIssuer(ISSUER).validate(expired).hasErrors())
                .isTrue();
    }

    @Test
    void rejectsTokenFromUntrustedIssuer() {
        Jwt foreignIssuer = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .issuer("https://evil.example/realms/vros")
                .audience(List.of("vros-api"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThat(JwtValidators.createDefaultWithIssuer(ISSUER).validate(foreignIssuer).hasErrors())
                .isTrue();
    }

    @Test
    void rejectsTokenWithoutExpectedAudience() {
        assertThat(validator.validate(jwtWithAudience(List.of("account"))).hasErrors())
                .isTrue();
        assertThat(validator.validate(jwtWithAudience(List.of("account"))).getErrors())
                .first()
                .satisfies(error -> {
                    assertThat(error.getErrorCode()).isEqualTo("invalid_token");
                    assertThat(error.getDescription()).contains("vros-api");
                });
    }
}
