package com.tournament.tournament_manager.exception.handler;

import com.tournament.tournament_manager.exception.domain.AlreadyExistsException;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.NotFoundException;
import com.tournament.tournament_manager.exception.domain.OpenAiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_shouldReturn404() {
        ProblemDetail problem = handler.handleNotFound(new NotFoundException("Player not found") {});
        assertEquals(404, problem.getStatus());
        assertEquals("Not Found", problem.getTitle());
        assertEquals("Player not found", problem.getDetail());
    }

    @Test
    void handleAlreadyExists_shouldReturn409() {
        ProblemDetail problem = handler.handleAlreadyExists(new AlreadyExistsException("Already exists") {});
        assertEquals(409, problem.getStatus());
    }

    @Test
    void handleInvalid_shouldReturn400() {
        ProblemDetail problem = handler.handleInvalid(new InvalidException("Invalid input"));
        assertEquals(400, problem.getStatus());
        assertEquals("Invalid input", problem.getDetail());
    }

    /**
     * Point 34 : vérifie que TOUTES les erreurs de validation sont renvoyées, pas juste
     * la première - avant, un formulaire avec 3 champs invalides ne révélait le 2e problème
     * qu'après avoir corrigé et renvoyé le 1er.
     */
    @Test
    void handleValidation_shouldReturnAllFieldErrors_notJustTheFirst() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "username", "must not be blank"));
        bindingResult.addError(new FieldError("object", "email", "must be a valid email"));
        bindingResult.addError(new FieldError("object", "maxPlayers", "must be a power of 2"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail problem = handler.handleValidation(ex);

        assertEquals(400, problem.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) problem.getProperties().get("errors");
        assertEquals(3, errors.size());
        assertTrue(errors.stream().anyMatch(e -> "username".equals(e.get("field"))));
        assertTrue(errors.stream().anyMatch(e -> "email".equals(e.get("field"))));
        assertTrue(errors.stream().anyMatch(e -> "maxPlayers".equals(e.get("field"))));
    }

    @Test
    void handleOpenAiUnavailable_shouldReturn503() {
        ProblemDetail problem = handler.handleOpenAiUnavailable(
                new OpenAiUnavailableException("OpenAI down", new RuntimeException()));
        assertEquals(503, problem.getStatus());
    }

    @Test
    void handleOptimisticLocking_shouldReturn409() {
        ProblemDetail problem = handler.handleOptimisticLocking(
                new ObjectOptimisticLockingFailureException(
                        com.tournament.tournament_manager.domain.model.Player.class, 1L));
        assertEquals(409, problem.getStatus());
        assertNotNull(problem.getDetail());
    }

    @Test
    void handleBadCredentials_shouldReturn401() {
        ProblemDetail problem = handler.handleBadCredentials(new BadCredentialsException("Bad credentials"));
        assertEquals(401, problem.getStatus());
        assertEquals("Identifiants invalides", problem.getDetail());
    }

    /**
     * Point 34 : handler manquant auparavant - un accès refusé (@PreAuthorize côté JSON-RPC,
     * voir point 25) tombait dans handleGeneric et ressortait en 500 générique.
     */
    @Test
    void handleAccessDenied_shouldReturn403() {
        ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("Access is denied"));
        assertEquals(403, problem.getStatus());
        assertNotNull(problem.getDetail());
    }

    @Test
    void handleGeneric_shouldReturn500() {
        ProblemDetail problem = handler.handleGeneric(new RuntimeException("Unexpected error"));
        assertEquals(500, problem.getStatus());
        assertNotNull(problem.getProperties().get("timestamp"));
    }
}
