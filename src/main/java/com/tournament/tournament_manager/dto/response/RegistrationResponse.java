package com.tournament.tournament_manager.dto.response;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long playerId,
        Long tournamentId,
        LocalDateTime registeredAt
) {}
