package com.tournament.tournament_manager.dto.request.tournament;

import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTournamentRequest(
        @NotBlank(message = "Le nom du tournoi est obligatoire")
        @Size(min = 3, max = 50, message = "Le nom du tournoi doit contenir entre 3 et 50 caractères")
        String name,

        @Min(value = 4, message = "Le tournoi doit avoir au moins 4 joueurs")
        @Max(value = 128, message = "Le tournoi ne peut pas dépasser 128 joueurs")
        int maxPlayers,

        TournamentFormat format,
        Integer numberOfGroups,
        Integer qualifiersPerGroup
) {
    public TournamentFormat format() {
        return format != null ? format : TournamentFormat.SINGLE_ELIMINATION;
    }
}
