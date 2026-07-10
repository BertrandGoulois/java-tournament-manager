package com.tournament.tournament_manager.exception.domain;

/**
 * Exception de base pour les ressources introuvables.
 * Mappée en {@code 404 Not Found} par le {@code GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
