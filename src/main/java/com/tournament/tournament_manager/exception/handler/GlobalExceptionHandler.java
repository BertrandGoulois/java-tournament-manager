package com.tournament.tournament_manager.exception.handler;

import com.tournament.tournament_manager.exception.domain.AlreadyExistsException;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.NotFoundException;
import com.tournament.tournament_manager.exception.domain.OpenAiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Intercepte toutes les exceptions et les traduit en réponses HTTP uniformes
 * via {@link ErrorResponse}.
 *
 * <ul>
 *   <li>{@link NotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link AlreadyExistsException} → {@code 409 Conflict}</li>
 *   <li>{@link InvalidException} → {@code 400 Bad Request}</li>
 *   <li>{@link MethodArgumentNotValidException} → {@code 400 Bad Request}</li>
 *   <li>{@link OpenAiUnavailableException} → {@code 503 Service Unavailable}</li>
 *   <li>{@link Exception} → {@code 500 Internal Server Error}</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(AlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(InvalidException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", message));
    }

    @ExceptionHandler(OpenAiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleOpenAiUnavailable(OpenAiUnavailableException ex) {
        log.warn("Service OpenAI indisponible : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(503, "Service Unavailable", "Le service de commentaire IA est temporairement indisponible"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Erreur inattendue : {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "Une erreur inattendue s'est produite"));
    }
}