package com.tournament.tournament_manager.dto.response;

import java.time.LocalDateTime;

public record PlayerResponse(
        Long id,
        String username,
        String email,
        int eloRating,
        LocalDateTime createdAt
) {}
