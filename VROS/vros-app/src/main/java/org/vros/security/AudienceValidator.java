package org.vros.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Rejects access tokens whose {@code aud} claim does not contain the VROS API audience.
 * Keycloak may emit the claim either as a single string or as a JSON array; the
 * {@link Jwt#getAudience()} accessor normalises both representations to a list.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;
    private final OAuth2Error error;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
        this.error = new OAuth2Error(
                "invalid_token",
                "The required audience '" + expectedAudience + "' is missing in token aud claim",
                null);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        if (audiences != null && audiences.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(error);
    }
}
