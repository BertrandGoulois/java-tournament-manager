package com.tournament.tournament_manager.dto.response.player;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record PlayerResponse(
        @Schema(example = "1") Long id,
        String username,
        String email,
        @Schema(example = "1000") int eloRating,
        Instant createdAt
) {}
