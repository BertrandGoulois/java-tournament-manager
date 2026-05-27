package com.tournament.tournament_manager.dto.response;

import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;

import java.util.List;

public record BracketResponse(
        Long tournamentId,
        String tournamentName,
        TournamentStatus status,
        List<BracketRoundResponse> rounds
) {}