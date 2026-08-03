package com.tournament.tournament_manager.domain.port.out.player;

/**
 * Port sortant : comptage des matchs et victoires d'un joueur.
 */
public interface CountMatchesByPlayerPort {

    /**
     * Compte le nombre de matchs <b>réellement joués</b> par un joueur : uniquement les
     * matchs {@code FINISHED} entre deux joueurs réels. Exclut les matchs {@code PENDING}
     * (programmés mais pas encore joués) et les byes (un seul joueur réel, l'autre absent).
     *
     * @param playerId identifiant du joueur
     * @return nombre de matchs réellement joués
     */
    long countByPlayer(Long playerId);

    /**
     * Compte le nombre de victoires <b>réelles</b> d'un joueur : mêmes exclusions que
     * {@link #countByPlayer} — un bye n'est pas une victoire (le joueur n'a battu personne).
     *
     * @param playerId identifiant du joueur
     * @return nombre de victoires réelles
     */
    long countWinsByPlayer(Long playerId);
}