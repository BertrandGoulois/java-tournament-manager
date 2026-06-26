package com.tournament.tournament_manager.dto.response.tournament;

/**
 * Ligne de classement d'un joueur dans un tournoi round-robin.
 */
public record StandingEntryResponse(
        Long playerId,
        String username,
        int matchesPlayed,
        int wins,
        int losses,
        int points
) {}