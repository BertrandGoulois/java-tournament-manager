package com.tournament.tournament_manager.dto.response.tournament;

import java.util.List;

/**
 * Classement complet d'un tournoi round-robin, trié par points décroissants.
 */
public record StandingsResponse(
        Long tournamentId,
        String tournamentName,
        List<StandingEntryResponse> standings
) {}