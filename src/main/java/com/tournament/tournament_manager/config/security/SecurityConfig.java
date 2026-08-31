package com.tournament.tournament_manager.config.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration Spring Security de l'application.
 *
 * <p>Endpoints publics (sans JWT) :
 * <ul>
 *   <li>{@code /api/auth/**} — authentification</li>
 *   <li>{@code /swagger-ui/**}, {@code /v3/api-docs/**} — documentation API</li>
 *   <li>{@code /ws/**}, {@code /ws-test.html} — WebSocket</li>
 *   <li>{@code /livez}, {@code /readyz} — probes liveness/readiness (actuator détaillé
 *       déplacé sur {@code management.server.port}, hors de ce filtre)</li>
 * </ul>
 * Tous les autres endpoints nécessitent un token JWT valide.
 * Les sessions HTTP sont désactivées (stateless).
 *
 * <p>{@code POST /api/rpc} n'a <b>pas</b> de règle ADMIN en bloc ici, contrairement à
 * l'ancienne version de ce fichier : Spring Security ne peut filtrer que par URL, jamais par
 * le contenu du corps JSON, donc il ne peut pas savoir depuis ce niveau si la méthode
 * JSON-RPC appelée (ex. {@code tournament.create}) exige ADMIN ou pas — la même URL sert les
 * 17 méthodes exposées, avec des exigences différentes selon la méthode. {@code /api/rpc}
 * retombe donc sur la règle par défaut (authentifié suffit), et chaque handler JSON-RPC dont
 * l'équivalent REST exige ADMIN porte sa propre {@code @PreAuthorize("hasRole('ADMIN')")} —
 * activée par {@code @EnableMethodSecurity} ci-dessous. Les deux canaux exposent enfin
 * exactement les mêmes règles d'autorisation pour la même opération métier.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final RateLimitingFilter rateLimitingFilter;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsRaw;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsServiceImpl userDetailsService,
                          RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/webjars/**",
                                "/ws/**",
                                "/ws-test.html",
                                "/error",
                                // Actuator tourne désormais sur management.server.port (séparé,
                                // non exposé par nginx) : seules les probes liveness/readiness
                                // restent publiques, sur le port principal.
                                "/livez",
                                "/readyz"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tournaments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/tournaments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tournaments/*/start").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/matches/*/result").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/players/**").hasRole("ADMIN")
                        // Pas de règle ADMIN en bloc ici pour /api/rpc — voir la Javadoc de
                        // cette classe : le contrôle se fait désormais par méthode JSON-RPC,
                        // via @PreAuthorize sur chaque handler concerné.
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuration CORS - absente auparavant (point 33 de la revue), ce qui bloquait
     * silencieusement toute requête cross-origin depuis un navigateur (aucun frontend de
     * ce type n'existe encore dans ce projet, mais l'absence totale de configuration
     * aurait surpris quiconque essayant d'en brancher un).
     *
     * <p>Liste blanche vide par défaut ({@code app.cors.allowed-origins}, non définie) :
     * aucune origine autorisée tant qu'elle n'est pas explicitement configurée - sûr par
     * défaut plutôt que permissif par défaut. {@code allowCredentials(true)} est
     * nécessaire puisque l'API utilise un header {@code Authorization: Bearer}, mais
     * {@code allowedOrigins("*")} est alors interdit par la spec CORS elle-même
     * (incompatible avec les credentials) - d'où une liste explicite plutôt qu'un
     * joker, même si elle finissait par contenir une seule valeur.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = allowedOriginsRaw == null || allowedOriginsRaw.isBlank()
                ? List.of()
                : Arrays.stream(allowedOriginsRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}