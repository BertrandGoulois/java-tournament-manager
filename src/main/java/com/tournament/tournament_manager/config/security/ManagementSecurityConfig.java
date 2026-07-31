package com.tournament.tournament_manager.config.security;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité du port de management (actuator), servi sur {@code management.server.port}
 * (voir {@code application.properties}), distinct du port applicatif principal.
 *
 * <p>Dès que Spring Security est sur le classpath, Spring Boot sécurise par défaut
 * l'intégralité des endpoints actuator sur ce port séparé (401 systématique) — y compris
 * {@code /actuator/prometheus}, ce qui empêche Prometheus de scraper les métriques.
 *
 * <p>On choisit ici de confier la protection de ce port au réseau plutôt qu'à
 * l'authentification applicative : il n'est jamais relayé par nginx (voir
 * {@code docker-compose.yml}), seuls le réseau interne docker-compose et l'hôte local
 * (pour le dev) peuvent l'atteindre. À armer d'une authentification HTTP Basic si ce
 * port devait un jour être joignable depuis un réseau moins maîtrisé.
 */
@Configuration
public class ManagementSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
