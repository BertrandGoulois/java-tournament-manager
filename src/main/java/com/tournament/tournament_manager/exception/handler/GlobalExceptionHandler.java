package com.tournament.tournament_manager.exception.handler;

import com.tournament.tournament_manager.exception.domain.AlreadyExistsException;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.NotFoundException;
import com.tournament.tournament_manager.exception.domain.OpenAiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Intercepte toutes les exceptions et les traduit en réponses HTTP uniformes, au format
 * {@link ProblemDetail} - RFC 7807 ("Problem Details for HTTP APIs"), supporté nativement
 * par Spring depuis la 6.0/Boot 3.0. Remplace l'ancien DTO maison {@code ErrorResponse}
 * (point 34 de la revue) : un format standard, avec les mêmes champs
 * ({@code status}/{@code title}/{@code detail}) que n'importe quel client HTTP générique
 * sait déjà interpréter, plutôt qu'un schéma propriétaire à documenter et maintenir.
 *
 * <p>Extension {@code timestamp} ajoutée à chaque réponse via {@link ProblemDetail#setProperty}
 * (RFC 7807 autorise explicitement des propriétés d'extension au-delà des 5 champs standard) -
 * pratique pour corréler une erreur avec les logs serveur.
 *
 * <p><b>Trois formats d'erreur coexistaient auparavant</b> (point 34) : {@code ErrorResponse}
 * ici pour tout ce qui passait par {@code @ControllerAdvice}, le format d'erreur par défaut
 * de Spring Boot (page {@code /error}) pour tout ce qui échouait au niveau du filtre de
 * sécurité (avant même d'atteindre un contrôleur - {@code authenticationEntryPoint}), et le
 * format JSON-RPC 2.0 pour {@code /api/rpc}. Ce dernier reste légitimement différent (protocole
 * distinct, avec sa propre spec) ; les deux premiers sont désormais unifiés sous
 * {@link ProblemDetail} - voir {@code SecurityConfig} pour la partie filtre de sécurité.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(AlreadyExistsException ex) {
        return problemDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidException.class)
    public ProblemDetail handleInvalid(InvalidException ex) {
        return problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Point 34 : renvoie désormais TOUTES les erreurs de validation en une seule réponse,
     * pas juste la première - {@code errors} liste chaque champ en échec avec son message.
     * Avant, une requête avec 3 champs invalides ne révélait le 2e problème qu'après avoir
     * corrigé et renvoyé le 1er, et ainsi de suite - un aller-retour par champ fautif.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();

        String summary = errors.stream()
                .map(e -> e.get("field") + " : " + e.get("message"))
                .reduce((a, b) -> a + " ; " + b)
                .orElse("Validation error");

        ProblemDetail problem = problemDetail(HttpStatus.BAD_REQUEST, summary);
        problem.setProperty("errors", errors);
        return problem;
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "message", fieldError.getDefaultMessage() != null
                        ? fieldError.getDefaultMessage()
                        : "invalid value"
        );
    }

    @ExceptionHandler(OpenAiUnavailableException.class)
    public ProblemDetail handleOpenAiUnavailable(OpenAiUnavailableException ex) {
        log.warn("Service OpenAI indisponible : {}", ex.getMessage());
        return problemDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Le service de commentaire IA est temporairement indisponible");
    }

    /**
     * Levée par Hibernate quand {@code @Version} détecte qu'une entité a été modifiée par
     * quelqu'un d'autre entre le chargement et l'écriture (deux requêtes concurrentes sur le
     * même joueur/tournoi/match). Sans ce handler, elle tombait dans {@link #handleGeneric} et
     * ressortait en 500 générique — un vrai conflit de concurrence, détecté correctement par
     * Hibernate, présenté à l'appelant comme un bug serveur plutôt que comme "réessaie, quelqu'un
     * d'autre est passé avant toi".
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        log.warn("Conflit de modification concurrente détecté : {}", ex.getMessage());
        return problemDetail(HttpStatus.CONFLICT,
                "Cette ressource a été modifiée entre-temps par quelqu'un d'autre. "
                        + "Recharge les données à jour et réessaie.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(Exception ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
    }

    /**
     * Point 34 : handler manquant auparavant - un utilisateur authentifié mais sans le rôle
     * requis (ex. PLAYER sur un endpoint réservé ADMIN protégé par
     * {@code @PreAuthorize}) tombait dans {@link #handleGeneric} et ressortait en 500, alors
     * que c'est un 403 attendu et normal. Ne couvre que les échecs d'autorisation déclenchés
     * depuis l'intérieur d'un contrôleur (ex. {@code @PreAuthorize} sur un handler JSON-RPC
     * appelé depuis {@code JsonRpcController}) - les échecs déclenchés plus tôt, au niveau du
     * filtre de sécurité (règles {@code .hasRole(...)} par URL sur les contrôleurs REST), sont
     * interceptés avant d'atteindre ce handler et traités par
     * {@code SecurityConfig.accessDeniedHandler}, qui produit le même format.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Accès refusé : {}", ex.getMessage());
        return problemDetail(HttpStatus.FORBIDDEN,
                "Vous n'avez pas les droits nécessaires pour cette opération");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Erreur inattendue : {}", ex.getMessage(), ex);
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue s'est produite");
    }

    private ProblemDetail problemDetail(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
