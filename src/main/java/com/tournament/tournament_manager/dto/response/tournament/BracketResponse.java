package com.tournament.tournament_manager.dto.response.tournament;

import java.util.List;

import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record BracketResponse(
        @Schema(example = "1") Long tournamentId,
        String tournamentName,
        TournamentStatus status,
        List<BracketRoundResponse> rounds
) {}
