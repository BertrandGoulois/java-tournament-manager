package com.tournament.tournament_manager.domain.port.out.auth;

import com.tournament.tournament_manager.domain.model.RefreshToken;

import java.util.Optional;

/**
 * Port sortant : chargement d'un refresh token depuis la persistance.
 */
public interface LoadRefreshTokenPort {

    /**
     * Charge un refresh token par sa valeur.
     *
     * @param token la valeur du token
     * @return le refresh token correspondant, vide si inexistant
     */
    Optional<RefreshToken> loadByToken(String token);
}