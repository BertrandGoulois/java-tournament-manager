package com.tournament.tournament_manager.domain.port.in;

import com.tournament.tournament_manager.dto.response.MatchResponse;

public interface GetMatchUseCase {
    MatchResponse getMatchById(Long id);
}