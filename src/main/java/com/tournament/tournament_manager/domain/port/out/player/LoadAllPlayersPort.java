package com.tournament.tournament_manager.domain.port.out.player;

import com.tournament.tournament_manager.domain.model.entities.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port sortant : chargement de tous les joueurs depuis la persistance.
 */
public interface LoadAllPlayersPort {

    /**
     * Charge tous les joueurs existants de façon paginée.
     *
     * @param pageable paramètres de pagination (page, taille, tri)
     * @return une page de joueurs
     */
    Page<Player> loadAllPlayers(Pageable pageable);
}