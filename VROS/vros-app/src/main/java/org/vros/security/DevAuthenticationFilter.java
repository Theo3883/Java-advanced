package org.vros.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Development-only filter that injects a synthetic ADMIN identity when the request is
 * anonymous. It is only registered by the {@code dev} filter chain and never in production.
 */
public class DevAuthenticationFilter extends OncePerRequestFilter {

    public static final String DEV_SUBJECT = "dev-bypass-subject";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Instant now = Instant.now();
            Jwt jwt = Jwt.withTokenValue("dev-bypass-token")
                    .header("alg", "none")
                    .subject(DEV_SUBJECT)
                    .claim("email", "dev@vros.local")
                    .claim("name", "VROS Dev User")
                    .claim("preferred_username", "dev_user")
                    .claim("realm_access", Map.of("roles", List.of("ADMIN", "CUSTOMER", "DRIVER")))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .build();

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                            new SimpleGrantedAuthority("ROLE_DRIVER")),
                    "dev_user");
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
