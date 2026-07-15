package com.tournament.tournament_manager.dto.request.match;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecordMatchResultRequest(
        @NotNull(message = "L'identifiant du vainqueur est obligatoire")
        @Positive(message = "L'identifiant du vainqueur doit être positif")
        Long winnerId
) {}
