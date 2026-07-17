package com.tournament.tournament_manager.dto.response.match;

import io.swagger.v3.oas.annotations.media.Schema;

public record MatchCommentaryResponse(
        @Schema(example = "1") Long matchId,
        String commentary
) {}