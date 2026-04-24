package com.tournament.tournament_manager.exception;

public class MatchNotFoundException extends NotFoundException{
    public MatchNotFoundException(Long id) {
        super("Match not found with id: " + id);
    }
}
