package com.tournament.tournament_manager.exception;

/**
 * Levée quand un tournoi est introuvable par son identifiant.
 */
public class TournamentNotFoundException extends NotFoundException {
    public TournamentNotFoundException(Long id) {
        super("Tournament not found with id: " + id);
    }
}
