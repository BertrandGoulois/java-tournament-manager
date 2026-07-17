package com.tournament.tournament_manager.dto.response.tournament;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ligne de classement d'un joueur dans un tournoi round-robin.
 */
public record StandingEntryResponse(
        @Schema(example = "1") Long playerId,
        String username,
        @Schema(example = "3") int matchesPlayed,
        @Schema(example = "2") int wins,
        @Schema(example = "1") int losses,
        @Schema(example = "6") int points
) {}