package org.vros.api.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.vros.security.KeycloakJwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Authenticates STOMP CONNECT frames and authorizes optimization subscriptions.
 *
 * <p>The HTTP handshake is public; the bearer token travels in the STOMP {@code Authorization}
 * native header. In the dev profile a synthetic ADMIN principal mirrors
 * {@code DevAuthenticationFilter}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> OPTIMIZATION_ROLES = Set.of("ROLE_ADMIN", "ROLE_DRIVER");

    private final JwtDecoder jwtDecoder;
    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("${vros.security.dev-bypass:false}")
    private boolean devBypass;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
            AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
            log.debug("STOMP CONNECT authenticated as {}", authentication.getName());
            return authentication;
        }
        if (devBypass) {
            log.debug("STOMP CONNECT accepted with dev-bypass principal");
            return devAuthentication();
        }
        throw new AccessDeniedException("STOMP CONNECT requires a Bearer token");
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/optimization")) {
            return;
        }
        Authentication authentication = (Authentication) accessor.getUser();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Subscribe requires an authenticated STOMP session");
        }
        boolean allowed = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(OPTIMIZATION_ROLES::contains);
        if (!allowed) {
            throw new AccessDeniedException(
                    "Subscribing to " + destination + " requires ADMIN or DRIVER");
        }
    }

    private Authentication devAuthentication() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("dev-stomp-token")
                .header("alg", "none")
                .subject("dev-stomp-subject")
                .claim("preferred_username", "dev_user")
                .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN", "DRIVER")))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        return new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_DRIVER")),
                "dev_user");
    }
}
