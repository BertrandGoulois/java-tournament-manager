package com.tournament.tournament_manager.dto.response.player;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlayerResponse(
        @Schema(example = "1") Long id,
        String username,
        String email,
        @Schema(example = "1000") int eloRating,
        Instant createdAt
) {}
