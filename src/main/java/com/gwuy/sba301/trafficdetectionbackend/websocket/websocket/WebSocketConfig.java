package com.gwuy.sba301.trafficdetectionbackend.websocket.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Configure the broker destination prefix
        config.enableSimpleBroker("/topic");
        // Application destination prefix for client-to-server messaging
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the endpoint and enable SockJS fallback options
        registry.addEndpoint("/traffic-ws")
                .setAllowedOriginPatterns("*") // Allow all origins temporarily
                .withSockJS();
    }
}
