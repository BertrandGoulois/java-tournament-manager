package com.tournament.tournament_manager.dto.response.match;

import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record MatchResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "4") int round,
        @Schema(example = "0") int position,
        MatchStatus status,
        LocalDateTime playedAt,
        @Schema(example = "1") Long tournamentId,
        @Schema(example = "1") Long player1Id,
        @Schema(example = "2") Long player2Id,
        @Schema(example = "1") Long winnerId
) {}
