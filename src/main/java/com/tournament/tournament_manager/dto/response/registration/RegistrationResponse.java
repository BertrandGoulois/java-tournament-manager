package com.tournament.tournament_manager.dto.response.registration;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegistrationResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long playerId,
        @Schema(example = "1") Long tournamentId,
        Instant registeredAt
) {}
