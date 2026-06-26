package com.tournament.tournament_manager.domain.port.in.auth;

import com.tournament.tournament_manager.dto.response.auth.AuthResponse;

/**
 * Port entrant : cas d'utilisation pour le refresh token JWT.
 */
public interface RefreshTokenUseCase {

    /**
     * Génère un nouveau access token à partir d'un refresh token valide.
     *
     * @param refreshToken le refresh token
     * @return un nouvel access token JWT
     * @throws com.tournament.tournament_manager.exception.InvalidException si le token est invalide, expiré ou révoqué
     */
    AuthResponse refresh(String refreshToken);

    /**
     * Révoque le refresh token (logout).
     *
     * @param refreshToken le refresh token à révoquer
     */
    void revoke(String refreshToken);
}