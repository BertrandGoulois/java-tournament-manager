package com.tournament.tournament_manager.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateRegistrationRequest(
    @NotNull Long playerId,
    @NotNull Long tournamentId
) {}
