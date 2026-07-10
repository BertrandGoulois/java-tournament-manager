package com.tournament.tournament_manager.exception.domain;

/**
 * Levée quand un tournoi existe déjà avec le même nom.
 */
public class TournamentAlreadyExistsException extends AlreadyExistsException {
    public TournamentAlreadyExistsException(String name) {
        super("Tournament already exists with name " + name);
    }
}
