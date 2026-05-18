package com.tournament.tournament_manager.domain.port.out;

/**
 * Port sortant : comptage des matchs et victoires d'un joueur.
 */
public interface CountMatchesByPlayerPort {

    /**
     * Compte le nombre total de matchs joués par un joueur.
     *
     * @param playerId identifiant du joueur
     * @return nombre de matchs joués
     */
    long countByPlayer(Long playerId);

    /**
     * Compte le nombre de victoires d'un joueur.
     *
     * @param playerId identifiant du joueur
     * @return nombre de victoires
     */
    long countWinsByPlayer(Long playerId);
}