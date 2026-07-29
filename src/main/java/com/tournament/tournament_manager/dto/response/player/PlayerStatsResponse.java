package com.tournament.tournament_manager.dto.response.player;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PlayerStatsResponse (
       @Schema(example = "1") Long id,
       String username,
       @Schema(example = "1000") int eloRating,
       @Schema(example = "10") int matchesPlayed,
       @Schema(example = "7") int wins,
       @Schema(example = "3") int losses,
       @Schema(example = "70.0") double winRate,
       List<EloHistoryResponse> eloHistory
) {}
