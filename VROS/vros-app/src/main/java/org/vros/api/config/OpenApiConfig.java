package org.vros.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * Springdoc OpenAPI configuration.
 * All user-facing metadata is resolved dynamically from Resource Bundles (MessageSource)
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final MessageSource messageSource;

    @Bean
    public OpenAPI vrosOpenApi() {
        Locale defaultLocale = Locale.ENGLISH;

        String title = messageSource.getMessage("openapi.title", null, defaultLocale);
        String description = messageSource.getMessage("openapi.description", null, defaultLocale);
        String version = messageSource.getMessage("openapi.version", null, defaultLocale);
        String contactName = messageSource.getMessage("openapi.contact.name", null, defaultLocale);
        String licenseName = messageSource.getMessage("openapi.license.name", null, defaultLocale);

        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version)
                        .contact(new Contact().name(contactName))
                        .license(new License().name(licenseName)));
    }
}
