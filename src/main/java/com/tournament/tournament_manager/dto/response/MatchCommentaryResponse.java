package com.tournament.tournament_manager.dto.response;

public record MatchCommentaryResponse(
        Long matchId,
        String commentary
) {}