package com.tournament.tournament_manager.domain.port.in.player;

import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;

/**
 * Port entrant : cas d'utilisation pour supprimer (soft delete) un joueur.
 */
public interface DeletePlayerUseCase {

    /**
     * Désactive un joueur sans le supprimer physiquement de la base.
     *
     * @param id identifiant du joueur
     * @throws PlayerNotFoundException si le joueur n'existe pas
     */
    void deletePlayer(Long id);
}