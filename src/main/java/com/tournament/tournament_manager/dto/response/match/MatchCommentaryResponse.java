package com.tournament.tournament_manager.dto.response.match;

public record MatchCommentaryResponse(
        Long matchId,
        String commentary
) {}