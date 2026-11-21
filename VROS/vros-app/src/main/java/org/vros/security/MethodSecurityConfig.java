package org.vros.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables method-level security ({@code @PreAuthorize}) for every profile except
 * {@code dev}. Keeping this separate from {@link SecurityConfig} is what makes the
 * dev bypass complete: in dev the security chain permits all requests
 * <em>and</em> no method-security advisor is registered.
 */
@Configuration
@Profile("!dev")
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
}
