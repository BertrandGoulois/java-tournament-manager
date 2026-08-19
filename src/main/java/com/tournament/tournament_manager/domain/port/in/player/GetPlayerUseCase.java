package com.tournament.tournament_manager.domain.port.in.player;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;

/**
 * Port entrant : cas d'utilisation pour consulter un ou plusieurs joueurs.
 */
public interface GetPlayerUseCase {
    /**
     * Retourne un joueur par son identifiant.
     *
     * @param id identifiant du joueur
     * @return le joueur
     * @throws PlayerNotFoundException si le joueur n'existe pas
     */
    Player getPlayerById(Long id);

    /**
     * Retourne une page de tous les joueurs.
     *
     * @return page de joueurs, vide si aucun joueur enregistré
     */
    PageResult<Player> getAllPlayers(PageRequest pageRequest);
}
