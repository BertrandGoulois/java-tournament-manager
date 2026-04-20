package com.tournament.tournament_manager.dto.response;

import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;

import java.time.LocalDateTime;

public record TournamentResponse(
        Long id,
        String name,
        TournamentStatus status,
        int maxPlayers,
        LocalDateTime createdAt
) {}
