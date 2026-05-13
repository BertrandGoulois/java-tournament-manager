package com.tournament.tournament_manager.exception;

/**
 * Levée quand {@code maxPlayers} n'est pas une puissance de 2.
 */
public class InvalidTournamentException extends InvalidException {
    public InvalidTournamentException(int value) {
        super("maxPlayers value: " + value + " is not a power of 2");
    }
}
