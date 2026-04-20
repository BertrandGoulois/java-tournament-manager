package com.tournament.tournament_manager.dto.response;

import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        int round,
        MatchStatus status,
        LocalDateTime playedAt,
        Long tournamentId,
        Long player1Id,
        Long player2Id,
        Long winnerId
) {}
