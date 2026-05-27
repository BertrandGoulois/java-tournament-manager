package com.tournament.tournament_manager.dto.response;

import java.util.List;

public record BracketRoundResponse(
        int round,
        List<BracketMatchResponse> matches
) {}