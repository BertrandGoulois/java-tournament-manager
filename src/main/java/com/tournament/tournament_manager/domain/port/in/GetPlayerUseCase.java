package com.tournament.tournament_manager.domain.port.in;

import com.tournament.tournament_manager.dto.response.PlayerResponse;

import java.util.List;

/**
 * Port entrant : cas d'utilisation pour consulter un ou plusieurs joueurs.
 */
public interface GetPlayerUseCase {
    /**
     * Retourne un joueur par son identifiant.
     *
     * @param id identifiant du joueur
     * @return la représentation du joueur
     * @throws com.tournament.tournament_manager.exception.PlayerNotFoundException si le joueur n'existe pas
     */
    PlayerResponse getPlayerById(Long id);

    /**
     * Retourne la liste de tous les joueurs.
     *
     * @return liste des joueurs, vide si aucun joueur enregistré
     */
    List<PlayerResponse> getAllPlayers();
}