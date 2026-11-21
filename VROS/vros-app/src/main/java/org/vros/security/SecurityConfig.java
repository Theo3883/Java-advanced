package org.vros.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless OAuth2 Resource Server configuration.
 *
 * <p>VROS never issues, signs or refreshes tokens: all access tokens are validated
 * against the Keycloak issuer (signature, expiry and audience). The {@code prod}
 * chain is fail-closed. The {@code dev} chain is an explicit, loudly-logged bypass
 * and is mutually exclusive with the production chain.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter jwtAuthConverter;
    private final SecurityProblemSupport problemSupport;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8081/realms/vros}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String configuredJwkSetUri;

    @Value("${vros.security.expected-audience:vros-api}")
    private String expectedAudience;

    @Bean
    @Profile("!dev")
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemSupport)
                        .accessDeniedHandler(problemSupport))
                .authorizeHttpRequests(auth -> auth
                        // Public documentation & liveness endpoints
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Customers: bulk listing/search/creation/deletion are administrative
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers", "/api/v1/customers/search").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasRole("ADMIN")

                        // Deliveries: bulk listing and deletion are administrative
                        .requestMatchers(HttpMethod.GET, "/api/v1/deliveries").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/deliveries/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/deliveries").authenticated()

                        // Vehicles: fleet discovery for ADMIN and DRIVER; mutations are ADMIN-only
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles", "/api/v1/vehicles/**").hasAnyRole("ADMIN", "DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/vehicles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/vehicles/**").hasRole("ADMIN")

                        // Scenarios: ADMIN & DRIVER can read; only ADMIN mutates
                        .requestMatchers(HttpMethod.GET, "/api/v1/scenarios", "/api/v1/scenarios/**").hasAnyRole("ADMIN", "DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/scenarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/scenarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/scenarios/**").hasRole("ADMIN")

                        // Self-service onboarding
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/me/customer-profile").hasRole("CUSTOMER")

                        // Everything else requires a valid Keycloak token; ownership is enforced by @PreAuthorize
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthConverter)
                                .decoder(jwtDecoder()))
                        .authenticationEntryPoint(problemSupport)
                        .accessDeniedHandler(problemSupport));

        return http.build();
    }

    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        LoggerFactory.getLogger(SecurityConfig.class).warn(
                "\u26A0\uFE0F  SECURITY WARNING: VROS is running with the 'dev' profile! "
                        + "Keycloak authentication and method security are BYPASSED. "
                        + "Do not deploy this profile to production!");

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new DevAuthenticationFilter(), AnonymousAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Builds the decoder without contacting Keycloak at startup (JWK set is fetched lazily
     * on the first token validation). This keeps contexts bootable in tests and dev.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = (configuredJwkSetUri != null && !configuredJwkSetUri.isBlank())
                ? configuredJwkSetUri
                : stripTrailingSlash(issuerUri) + "/protocol/openid-connect/certs";

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(expectedAudience);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "https://*.vros.org"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "If-None-Match"));
        config.setExposedHeaders(List.of("Location", "ETag"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }
}
