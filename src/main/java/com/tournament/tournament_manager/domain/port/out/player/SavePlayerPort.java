package com.tournament.tournament_manager.domain.port.out.player;

import com.tournament.tournament_manager.domain.model.entities.Player;

/**
 * Port sortant : sauvegarde d'un joueur en persistance.
 */
public interface SavePlayerPort {

    /**
     * Persiste un joueur et retourne l'entité sauvegardée.
     *
     * @param player le joueur à sauvegarder
     * @return le joueur sauvegardé
     */
    Player savePlayer(Player player);
}