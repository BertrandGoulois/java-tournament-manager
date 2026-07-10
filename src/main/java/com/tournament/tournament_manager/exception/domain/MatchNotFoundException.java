package com.tournament.tournament_manager.exception.domain;

/**
 * Levée quand un match est introuvable par son identifiant.
 */
public class MatchNotFoundException extends NotFoundException{
    public MatchNotFoundException(Long id) {
        super("Match not found with id: " + id);
    }
}
