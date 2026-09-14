package com.tournament.tournament_manager.dto.response.tournament;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Classement complet d'un tournoi round-robin, trié par points décroissants.
 */
public record StandingsResponse(
        @Schema(example = "1") Long tournamentId,
        String tournamentName,
        List<StandingEntryResponse> standings
) {}
