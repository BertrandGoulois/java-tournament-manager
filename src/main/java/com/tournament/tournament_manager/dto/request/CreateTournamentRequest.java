package com.tournament.tournament_manager.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateTournamentRequest(
        @NotBlank String name,
        @Min(4) int maxPlayers
) {
}
