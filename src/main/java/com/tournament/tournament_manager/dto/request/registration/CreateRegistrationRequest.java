package com.tournament.tournament_manager.dto.request.registration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRegistrationRequest(
        @NotNull(message = "L'identifiant du joueur est obligatoire")
        @Positive(message = "L'identifiant du joueur doit être positif")
        Long playerId,

        @NotNull(message = "L'identifiant du tournoi est obligatoire")
        @Positive(message = "L'identifiant du tournoi doit être positif")
        Long tournamentId
) {}
