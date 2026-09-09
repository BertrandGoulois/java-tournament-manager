package com.tournament.tournament_manager.config.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

/**
 * Filtre HTTP exécuté une seule fois par requête.
 *
 * <p>Intercepte le header {@code Authorization: Bearer <token>}, valide le token JWT et
 * injecte l'authentification dans le {@code SecurityContextHolder} si le token est valide.
 *
 * <p><b>Un token invalide n'interrompt jamais la chaîne de filtres.</b> Le filtre se contente
 * de ne pas authentifier ; c'est ensuite {@code AuthenticationEntryPoint} (voir
 * {@link SecurityConfig}) qui produit un 401 — et uniquement sur les routes protégées.
 * La version précédente écrivait le 401 elle-même et arrêtait la chaîne, sur <i>toutes</i>
 * les routes : un client dont le token venait d'expirer et qui appelait
 * {@code POST /api/auth/login} — l'endpoint public existant précisément pour se
 * ré-authentifier — recevait un 401 avant même d'atteindre le contrôleur, en gardant
 * simplement son ancien header {@code Authorization}. Impasse dont on ne sortait qu'en
 * supprimant le header à la main.
 *
 * <p>Trois catégories d'échec sont distinguées :
 * <ul>
 *   <li><b>Token illisible, mal signé ou expiré</b> ({@link JwtException},
 *       {@link IllegalArgumentException}) — pas d'authentification, la chaîne continue.</li>
 *   <li><b>Utilisateur inconnu</b> ({@link UsernameNotFoundException}, compte supprimé
 *       depuis l'émission du token) — même traitement.</li>
 *   <li><b>Panne technique</b> (base injoignable, pool épuisé…) — 503 explicite au format
 *       ProblemDetail. La version précédente attrapait tout dans un {@code catch (Exception)}
 *       et renvoyait « Token invalide » en 401 : une panne Postgres se présentait au client
 *       comme un problème d'authentification, et faisait grimper le taux de 401 en
 *       supervision au lieu de lever une alerte de disponibilité.</li>
 * </ul>
 *
 * <p>Le corps d'erreur suit RFC 7807 ({@code application/problem+json}), comme partout
 * ailleurs dans le projet (point 34 de la revue) — ce filtre écrivait auparavant un JSON
 * maison, dernier îlot non unifié du format d'erreur.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsServiceImpl userDetailsService,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    /**
     * Extrait le token JWT du header {@code Authorization}, le valide, et positionne
     * l'authentification dans le contexte de sécurité Spring.
     *
     * <p>Si le header est absent, ne commence pas par {@code "Bearer "}, ou porte un token
     * invalide, la requête est transmise au filtre suivant sans authentification.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            authenticate(request, token);
        } catch (JwtException | IllegalArgumentException e) {
            // Token expiré, signature invalide, format illisible : on n'authentifie pas et on
            // laisse la chaîne décider. Route publique -> la requête passe ; route protégée ->
            // l'entry point renvoie un 401 ProblemDetail.
            log.debug("Token JWT rejeté, requête poursuivie sans authentification : {}", e.getMessage());
        } catch (UsernameNotFoundException e) {
            // Token cryptographiquement valide mais dont le compte n'existe plus.
            log.debug("Utilisateur inconnu pour un token par ailleurs valide, requête poursuivie "
                    + "sans authentification");
        } catch (Exception e) {
            // Tout le reste est une panne d'infrastructure, pas un problème d'authentification.
            log.error("Échec technique pendant la validation du token JWT", e);
            writeProblemDetail(response, HttpStatus.SERVICE_UNAVAILABLE,
                    "Service d'authentification temporairement indisponible");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        String username = jwtService.extractUsername(token);
        if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(token, userDetails.getUsername())) {
            return;
        }
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void writeProblemDetail(HttpServletResponse response, HttpStatus status, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("timestamp", Instant.now());
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
