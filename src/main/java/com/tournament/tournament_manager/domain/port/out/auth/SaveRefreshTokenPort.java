package com.tournament.tournament_manager.domain.port.out.auth;

import com.tournament.tournament_manager.domain.model.entities.RefreshToken;

/**
 * Port sortant : sauvegarde d'un refresh token en persistance.
 */
public interface SaveRefreshTokenPort {

    /**
     * Persiste un refresh token.
     *
     * @param refreshToken le token à sauvegarder
     * @return le token sauvegardé
     */
    RefreshToken saveRefreshToken(RefreshToken refreshToken);
}