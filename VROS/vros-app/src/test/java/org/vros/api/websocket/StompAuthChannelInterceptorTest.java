package org.vros.api.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.vros.security.KeycloakJwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompAuthChannelInterceptorTest {

    private JwtDecoder jwtDecoder;
    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        interceptor = new StompAuthChannelInterceptor(
                jwtDecoder, new KeycloakJwtAuthenticationConverter("vros-api"));
        ReflectionTestUtils.setField(interceptor, "devBypass", false);
    }

    @Test
    void connectWithBearerTokenSetsJwtPrincipal() {
        Jwt jwt = jwt(List.of("ADMIN"));
        when(jwtDecoder.decode("good-token")).thenReturn(jwt);
        StompHeaderAccessor accessor = connectAccessor("Bearer good-token");

        interceptor.preSend(message(accessor), mock(MessageChannel.class));

        Authentication authentication = (Authentication) accessor.getUser();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_ADMIN");
    }

    @Test
    void connectWithoutTokenIsDeniedOutsideDev() {
        StompHeaderAccessor accessor = connectAccessor(null);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void connectWithoutTokenUsesDevPrincipalWhenBypassIsOn() {
        ReflectionTestUtils.setField(interceptor, "devBypass", true);
        StompHeaderAccessor accessor = connectAccessor(null);

        interceptor.preSend(message(accessor), mock(MessageChannel.class));

        Authentication authentication = (Authentication) accessor.getUser();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_ADMIN", "ROLE_DRIVER");
    }

    @Test
    void subscribeToOptimizationTopicRequiresAuthenticatedUser() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/optimization/" + UUID.randomUUID());

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void driverMaySubscribeToOptimizationTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/optimization/" + UUID.randomUUID());
        accessor.setUser(new JwtAuthenticationToken(
                jwt(List.of("DRIVER")),
                List.of(new SimpleGrantedAuthority("ROLE_DRIVER")),
                "driver"));

        interceptor.preSend(message(accessor), mock(MessageChannel.class));

        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    void customerMayNotSubscribeToOptimizationTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/optimization/" + UUID.randomUUID());
        accessor.setUser(new JwtAuthenticationToken(
                jwt(List.of("CUSTOMER")),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                "customer"));

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), mock(MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private StompHeaderAccessor connectAccessor(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return accessor;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Jwt jwt(List<String> roles) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .claim("preferred_username", "tester")
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
    }
}
