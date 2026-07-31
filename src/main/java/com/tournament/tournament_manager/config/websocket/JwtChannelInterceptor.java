package com.tournament.tournament_manager.config.websocket;

import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.config.security.UserDetailsServiceImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Intercepteur du canal d'entrée STOMP : authentifie chaque trame {@code CONNECT}
 * via un JWT porté par l'en-tête STOMP natif {@code Authorization: Bearer <token>},
 * et refuse toute trame {@code SUBSCRIBE} tant qu'aucun principal n'a été établi.
 *
 * <p>Le handshake HTTP {@code /ws} (SockJS) reste public — {@code SecurityConfig}
 * ne peut pas y lire un header {@code Authorization} de façon fiable selon les
 * clients. L'authentification réelle a lieu ici, au niveau du protocole STOMP,
 * une fois la session WebSocket établie.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtChannelInterceptor(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(command) && accessor.getUser() == null) {
            throw new MessagingException("Abonnement WebSocket refusé : session non authentifiée.");
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new MessagingException("Connexion WebSocket refusée : token JWT manquant.");
        }
        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(token, userDetails.getUsername())) {
                throw new MessagingException("Connexion WebSocket refusée : token JWT invalide ou expiré.");
            }
            return new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            throw new MessagingException("Connexion WebSocket refusée : token JWT invalide.", e);
        }
    }
}
