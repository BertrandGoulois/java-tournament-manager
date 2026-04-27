package com.tournament.tournament_manager.dto.response;

import java.time.LocalDateTime;

public record EloHistoryResponse(
        int eloChange,
        int eloAfter,
        LocalDateTime createdAt,
        Long matchId
) {}
