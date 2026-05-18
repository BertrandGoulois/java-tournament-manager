package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Player;

/**
 * Port sortant : chargement d'un joueur depuis la persistance.
 */
public interface LoadPlayerPort {

    /**
     * Charge un joueur par son identifiant.
     *
     * @param id identifiant du joueur
     * @return le joueur correspondant
     * @throws com.tournament.tournament_manager.exception.PlayerNotFoundException si le joueur n'existe pas
     */
    Player loadPlayer(Long id);
}