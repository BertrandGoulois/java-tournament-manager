package com.tournament.tournament_manager.dto.request.tournament;

import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateTournamentRequest(
        @NotBlank String name,
        @Min(4) int maxPlayers,
        TournamentFormat format,
        Integer numberOfGroups,
        Integer qualifiersPerGroup
) {
    public TournamentFormat format() {
        return format != null ? format : TournamentFormat.SINGLE_ELIMINATION;
    }
}
