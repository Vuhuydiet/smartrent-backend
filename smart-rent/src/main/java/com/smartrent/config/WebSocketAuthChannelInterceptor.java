package com.smartrent.config;

import com.smartrent.config.security.CustomJwtDecoder;
import com.smartrent.util.JwtRecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Authenticates the STOMP CONNECT frame and pins a {@link Principal} (the
 * recipientId resolved by {@link JwtRecipientResolver}) to the WebSocket
 * session. Notifications are then routed with
 * {@code convertAndSendToUser(recipientId, "/queue/notifications", ...)} to that
 * one session's private user-destination.
 *
 * <p>This closes the notification IDOR: previously the server broadcast to a
 * guessable public topic {@code /topic/notifications/{userId}} and the STOMP
 * layer skipped auth entirely, so any anonymous client could subscribe to any
 * user's realtime notifications. The bearer token is read from the CONNECT
 * frame's native {@code Authorization} header (the SockJS/STOMP client already
 * sends it) and validated with the same {@link CustomJwtDecoder} used by the
 * REST layer, so signature + invalidated-token checks stay consistent. A missing
 * or invalid token rejects the CONNECT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final CustomJwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Rejected STOMP CONNECT: missing bearer token");
            throw new MessagingException("Missing bearer token on STOMP CONNECT");
        }

        try {
            Jwt jwt = jwtDecoder.decode(authHeader.substring(7));
            String recipientId = JwtRecipientResolver.resolveRecipientId(jwt);
            accessor.setUser(new StompPrincipal(recipientId));
            log.debug("STOMP CONNECT authenticated for principal {}", recipientId);
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Rejected STOMP CONNECT: invalid token ({})", e.getMessage());
            throw new MessagingException("Invalid token on STOMP CONNECT", e);
        }

        return message;
    }

    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
