package com.tournament.tournament_manager.domain.port.out.player;

import com.tournament.tournament_manager.domain.model.Player;

/**
 * Port sortant : soft delete d'un joueur en persistance.
 */
public interface SoftDeletePlayerPort {

    /**
     * Marque un joueur comme supprimé et persiste la modification.
     *
     * @param player le joueur à désactiver
     */
    void softDeletePlayer(Player player);
}