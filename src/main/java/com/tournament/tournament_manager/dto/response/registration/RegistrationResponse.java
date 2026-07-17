package com.tournament.tournament_manager.dto.response.registration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record RegistrationResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long playerId,
        @Schema(example = "1") Long tournamentId,
        LocalDateTime registeredAt
) {}
