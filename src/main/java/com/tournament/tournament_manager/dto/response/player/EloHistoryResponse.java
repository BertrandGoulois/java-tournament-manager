package com.tournament.tournament_manager.dto.response.player;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

public record EloHistoryResponse(
        @Schema(example = "24") int eloChange,
        @Schema(example = "1024") int eloAfter,
        LocalDateTime createdAt,
        @Schema(example = "1") Long matchId
) implements Serializable {}
