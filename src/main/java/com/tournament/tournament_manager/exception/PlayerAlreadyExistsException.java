package com.tournament.tournament_manager.exception;

/**
 * Levée quand un joueur existe déjà avec le même username ou email.
 */
public class PlayerAlreadyExistsException extends AlreadyExistsException {
    public PlayerAlreadyExistsException(String field, String value) {
        super("Player already exists with " + field + ": " + value);
    }
}
