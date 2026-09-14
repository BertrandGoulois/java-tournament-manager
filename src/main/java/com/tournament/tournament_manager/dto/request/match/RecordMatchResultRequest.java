package com.tournament.tournament_manager.dto.request.match;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

public record RecordMatchResultRequest(
        @NotNull(message = "L'identifiant du vainqueur est obligatoire")
        @Positive(message = "L'identifiant du vainqueur doit être positif")
        @Schema(example = "1")
        Long winnerId
) {}
