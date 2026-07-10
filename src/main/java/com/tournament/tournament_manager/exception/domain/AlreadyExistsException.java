package com.tournament.tournament_manager.exception.domain;

/**
 * Exception de base pour les ressources déjà existantes.
 * Mappée en {@code 409 Conflict} par le {@code GlobalExceptionHandler}.
 */
public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String message) {
        super(message);
    }
}
