package com.tournament.tournament_manager.exception;

/**
 * Exception de base pour les opérations métier invalides.
 * Mappée en {@code 400 Bad Request} par le {@code GlobalExceptionHandler}.
 */
public class InvalidException extends RuntimeException {
    public InvalidException(String message) {
        super(message);
    }
}
