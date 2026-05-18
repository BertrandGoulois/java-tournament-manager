package com.tournament.tournament_manager.domain.port.out;

/**
 * Port sortant : vérification d'existence d'un joueur.
 */
public interface ExistsPlayerPort {

    /**
     * Vérifie si un joueur existe avec ce username.
     *
     * @param username le username à vérifier
     * @return {@code true} si un joueur avec ce username existe déjà
     */
    boolean existsByUsername(String username);

    /**
     * Vérifie si un joueur existe avec cet email.
     *
     * @param email l'email à vérifier
     * @return {@code true} si un joueur avec cet email existe déjà
     */
    boolean existsByEmail(String email);
}