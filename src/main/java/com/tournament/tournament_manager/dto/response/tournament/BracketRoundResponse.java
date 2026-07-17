package com.tournament.tournament_manager.dto.response.tournament;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record BracketRoundResponse(
        @Schema(example = "4") int round,
        List<BracketMatchResponse> matches
) {}