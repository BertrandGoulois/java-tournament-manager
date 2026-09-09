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
 * l'authentification applicative : il n'est jamais relayé par nginx et n'est plus publié
 * sur l'hôte (voir {@code docker-compose.yml}), seul le réseau interne docker-compose peut
 * donc l'atteindre — c'est ainsi que Prometheus le scrape.
 *
 * <p><b>Point 1.6 de la revue.</b> Cette justification était fausse jusqu'ici : le compose
 * contenait {@code ports: "9001:9001"}, qui publiait le port sur l'hôte. Sur toute machine
 * un tant soit peu exposée, {@code /actuator/metrics} et {@code /actuator/prometheus}
 * étaient donc lisibles sans authentification. La publication a été retirée plutôt que
 * d'armer le HTTP Basic : Prometheus passe par le réseau interne, personne d'autre n'a de
 * raison légitime d'atteindre ce port. À basculer sur HTTP Basic le jour où ce port devrait
 * être joignable depuis un réseau moins maîtrisé.
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
