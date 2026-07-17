package com.tournament.tournament_manager.dto.response.tournament;

import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record BracketMatchResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long player1Id,
        @Schema(example = "2") Long player2Id,
        @Schema(example = "1") Long winnerId,
        MatchStatus status
) {}