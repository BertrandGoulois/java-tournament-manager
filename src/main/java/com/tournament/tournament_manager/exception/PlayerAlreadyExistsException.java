package com.tournament.tournament_manager.exception;

public class PlayerAlreadyExistsException extends RuntimeException {
    public PlayerAlreadyExistsException(String field, String value) {
        super("Player already exists with " + field + ": " + value);
    }
}
