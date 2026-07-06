package com.smartrent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory broker. "/queue" is required so per-user destinations
        // (convertAndSendToUser → /user/{session}/queue/...) can be delivered;
        // "/topic" is kept for any pub/sub broadcast use.
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages bound from client to server
        config.setApplicationDestinationPrefixes("/app");
        // Per-user destination prefix — clients subscribe to /user/queue/... and
        // the framework routes to the session whose authenticated Principal matches.
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback. The HTTP handshake stays open
        // (permitAll in SecurityConfig); authentication happens at the STOMP layer
        // via authChannelInterceptor on the CONNECT frame.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Authenticate the STOMP CONNECT and pin the user's Principal to the session
        // so notifications route only to their own /user destination (closes the
        // notification IDOR).
        registration.interceptors(authChannelInterceptor);
    }
}
