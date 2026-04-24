package com.tournament.tournament_manager.dto.request;

import jakarta.validation.constraints.NotNull;

public record RecordMatchResultRequest(
        @NotNull Long winnerId
) {}
