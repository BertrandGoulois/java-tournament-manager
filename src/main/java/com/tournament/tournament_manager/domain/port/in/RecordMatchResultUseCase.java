package com.tournament.tournament_manager.domain.port.in;

import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;

public interface RecordMatchResultUseCase {
    MatchResponse recordMatchResult(Long matchId, RecordMatchResultRequest request);
}