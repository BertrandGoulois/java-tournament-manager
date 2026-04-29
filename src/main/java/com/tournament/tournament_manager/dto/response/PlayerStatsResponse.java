package com.tournament.tournament_manager.dto.response;

import java.io.Serializable;
import java.util.List;

public record PlayerStatsResponse (
       Long id,
       String username,
       int eloRating,
       int matchesPlayed,
       int wins,
       int losses,
       double winRate,
       List<EloHistoryResponse> eloHistory
) implements Serializable {}
