package com.tournament.tournament_manager.dto.response.tournament;

import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

public record BracketMatchResponse(
        Long id,
        Long player1Id,
        Long player2Id,
        Long winnerId,
        MatchStatus status
) {}