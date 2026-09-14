package com.tournament.tournament_manager.infrastructure.input.mapper;

import org.springframework.stereotype.Component;

import com.tournament.tournament_manager.domain.model.AuthResult;
import com.tournament.tournament_manager.dto.response.auth.AuthResponse;

/**
 * Convertit entre le domaine pur ({@link AuthResult}) et le DTO REST. Voir la Javadoc de
 * {@code PlayerRestMapper}.
 */
@Component
public class AuthRestMapper {

    public AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(result.accessToken(), result.refreshToken());
    }
}
