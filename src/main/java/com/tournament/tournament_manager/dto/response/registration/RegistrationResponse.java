package com.tournament.tournament_manager.dto.response.registration;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long playerId,
        Long tournamentId,
        LocalDateTime registeredAt
) {}
