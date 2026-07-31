package com.tournament.tournament_manager.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration du broker WebSocket STOMP.
 *
 * <p>Endpoint de connexion : {@code /ws} (avec fallback SockJS).
 * Topics de diffusion : préfixe {@code /topic} (ex. {@code /topic/matches}).
 * Préfixe des destinations applicatives : {@code /app}.
 *
 * <p>Authentification : {@link JwtChannelInterceptor} valide le JWT porté par la
 * trame STOMP {@code CONNECT} et refuse tout {@code SUBSCRIBE} non authentifié
 * (voir sa Javadoc).
 *
 * <p>Limite connue : {@code enableSimpleBroker} est un broker en mémoire, propre à
 * chaque instance. En cas de scale-out multi-instances, un client connecté à
 * l'instance A ne recevra pas les notifications consommées par l'instance B — il
 * faudrait un relais vers un vrai broker (RabbitMQ, ActiveMQ...) pour un
 * fonctionnement correct à plusieurs instances.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
