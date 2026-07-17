package com.tournament.tournament_manager.dto.response.tournament;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Classement complet d'un tournoi round-robin, trié par points décroissants.
 */
public record StandingsResponse(
        @Schema(example = "1") Long tournamentId,
        String tournamentName,
        List<StandingEntryResponse> standings
) {}