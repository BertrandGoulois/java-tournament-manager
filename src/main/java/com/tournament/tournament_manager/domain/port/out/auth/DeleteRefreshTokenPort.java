package com.tournament.tournament_manager.domain.port.out.auth;

/**
 * Port sortant : suppression des refresh tokens d'un utilisateur.
 */
public interface DeleteRefreshTokenPort {

    /**
     * Supprime tous les refresh tokens d'un utilisateur.
     *
     * @param username le username de l'utilisateur
     */
    void deleteByUsername(String username);
}