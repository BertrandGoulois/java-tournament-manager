package com.tournament.tournament_manager.domain.port.in.auth;

import com.tournament.tournament_manager.domain.model.AuthResult;
import com.tournament.tournament_manager.exception.domain.InvalidException;

/**
 * Port entrant : cas d'utilisation pour le refresh token JWT.
 */
public interface RefreshTokenUseCase {

    /**
     * Génère un nouveau access token à partir d'un refresh token valide.
     *
     * @param refreshToken le refresh token
     * @return un nouvel access token JWT et un nouveau refresh token
     * @throws InvalidException si le token est invalide, expiré ou révoqué
     */
    AuthResult refresh(String refreshToken);

    /**
     * Révoque le refresh token (logout).
     *
     * @param refreshToken le refresh token à révoquer
     */
    void revoke(String refreshToken);
}
