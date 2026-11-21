package org.vros.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * Springdoc OpenAPI configuration.
 * All user-facing metadata is resolved dynamically from Resource Bundles (MessageSource),
 * and the OAuth2 password flow points at the Keycloak token endpoint.
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "KeycloakOIDC";

    private final MessageSource messageSource;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8081/realms/vros}")
    private String issuerUri;

    @Bean
    public OpenAPI vrosOpenApi() {
        Locale defaultLocale = Locale.ENGLISH;

        String title = messageSource.getMessage("openapi.title", null, defaultLocale);
        String description = messageSource.getMessage("openapi.description", null, defaultLocale);
        String version = messageSource.getMessage("openapi.version", null, defaultLocale);
        String contactName = messageSource.getMessage("openapi.contact.name", null, defaultLocale);
        String licenseName = messageSource.getMessage("openapi.license.name", null, defaultLocale);

        String tokenUrl = stripTrailingSlash(issuerUri) + "/protocol/openid-connect/token";

        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version)
                        .contact(new Contact().name(contactName))
                        .license(new License().name(licenseName)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Keycloak OIDC authentication (bearer access tokens)")
                                .flows(new OAuthFlows()
                                        .password(new OAuthFlow()
                                                .tokenUrl(tokenUrl)
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID Connect identity"))
                                        )
                                )
                        ));
    }

    private static String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }
}
