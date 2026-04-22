package com.tournament.tournament_manager.exception;

public class InvalidTournamentException extends InvalidException {
    public InvalidTournamentException(int value) {
        super("maxPlayers value: " + value + " is not a power of 2");
    }
}
