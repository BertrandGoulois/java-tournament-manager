package com.tournament.tournament_manager.dto.response.auth;

public record AuthResponse(
        String token,
        String refreshToken
) {}
