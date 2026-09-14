package com.tournament.tournament_manager.dto.response.tournament;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record BracketRoundResponse(
        @Schema(example = "4") int round,
        List<BracketMatchResponse> matches
) {}
