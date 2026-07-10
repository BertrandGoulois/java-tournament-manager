package com.tournament.tournament_manager.domain.port.in.player;

import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port entrant : cas d'utilisation pour consulter un ou plusieurs joueurs.
 */
public interface GetPlayerUseCase {
    /**
     * Retourne un joueur par son identifiant.
     *
     * @param id identifiant du joueur
     * @return la représentation du joueur
     * @throws PlayerNotFoundException si le joueur n'existe pas
     */
    PlayerResponse getPlayerById(Long id);

    /**
     * Retourne une page de tous les joueurs.
     *
     * @return page de joueurs, vide si aucun joueur enregistré
     */
    Page<PlayerResponse> getAllPlayers(Pageable pageable);
}