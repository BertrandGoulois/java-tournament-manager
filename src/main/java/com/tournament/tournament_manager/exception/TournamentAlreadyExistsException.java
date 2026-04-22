package com.tournament.tournament_manager.exception;

public class TournamentAlreadyExistsException extends AlreadyExistsException {
    public TournamentAlreadyExistsException(String name) {
        super("Tournament already exists with name " + name);
    }
}
