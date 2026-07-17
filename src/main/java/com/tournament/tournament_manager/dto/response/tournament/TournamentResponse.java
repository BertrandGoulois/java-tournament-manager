package com.tournament.tournament_manager.dto.response.tournament;

import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record TournamentResponse(
        @Schema(example = "1") Long id,
        String name,
        TournamentStatus status,
        TournamentFormat format,
        @Schema(example = "8") int maxPlayers,
        @Schema(example = "2") Integer numberOfGroups,
        @Schema(example = "2") Integer qualifiersPerGroup,
        LocalDateTime createdAt
) {}
