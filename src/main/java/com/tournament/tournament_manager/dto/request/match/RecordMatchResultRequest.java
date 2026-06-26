package com.tournament.tournament_manager.dto.request.match;

import jakarta.validation.constraints.NotNull;

public record RecordMatchResultRequest(
        @NotNull Long winnerId
) {}
