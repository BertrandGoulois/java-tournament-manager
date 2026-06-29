package com.tournament.tournament_manager.dto.response.tournament;

import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;

import java.time.LocalDateTime;

public record TournamentResponse(
        Long id,
        String name,
        TournamentStatus status,
        TournamentFormat format,
        int maxPlayers,
        Integer numberOfGroups,
        Integer qualifiersPerGroup,
        LocalDateTime createdAt
) {}
