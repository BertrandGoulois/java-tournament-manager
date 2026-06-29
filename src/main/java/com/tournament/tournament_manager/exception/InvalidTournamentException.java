package com.tournament.tournament_manager.exception;

/**
 * Levée quand les paramètres de configuration d'un tournoi sont invalides
 * (puissance de 2 pour maxPlayers, ou configuration incohérente des groupes
 * pour le format GROUPS_THEN_KNOCKOUT).
 */
public class InvalidTournamentException extends InvalidException {
    public InvalidTournamentException(int value) {
        super("maxPlayers value: " + value + " is not a power of 2");
    }

    public InvalidTournamentException(String message) {
        super(message);
    }
}