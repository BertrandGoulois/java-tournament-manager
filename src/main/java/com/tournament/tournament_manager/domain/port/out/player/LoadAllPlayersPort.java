package com.tournament.tournament_manager.domain.port.out.player;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Player;

/**
 * Port sortant : chargement de tous les joueurs depuis la persistance.
 */
public interface LoadAllPlayersPort {

    /**
     * Charge tous les joueurs existants de façon paginée.
     *
     * @param pageRequest paramètres de pagination (page, taille)
     * @return une page de joueurs
     */
    PageResult<Player> loadAllPlayers(PageRequest pageRequest);
}
