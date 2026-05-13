package com.tournament.tournament_manager.exception;

/**
 * Levée quand un joueur est introuvable par son identifiant.
 */
public class PlayerNotFoundException extends NotFoundException {
    public PlayerNotFoundException(Long id) {
        super("Player not found with id: " + id);
    }
}
