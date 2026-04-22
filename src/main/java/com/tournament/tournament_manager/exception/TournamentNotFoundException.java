package com.tournament.tournament_manager.exception;

public class TournamentNotFoundException extends NotFoundException {
    public TournamentNotFoundException(Long id) {
        super("Tournament not found with id: " + id);
    }
}
