package com.tournament.tournament_manager.exception;

public class PlayerNotFoundException extends NotFoundException {
    public PlayerNotFoundException(Long id) {
        super("Player not found with id: " + id);
    }
}
