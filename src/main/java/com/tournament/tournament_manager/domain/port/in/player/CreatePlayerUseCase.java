package com.tournament.tournament_manager.domain.port.in.player;

import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.PlayerResponse;

/**
 * Port entrant : cas d'utilisation pour créer un joueur.
 */
public interface CreatePlayerUseCase {
    /**
     * Crée un nouveau joueur avec un classement ELO par défaut.
     *
     * @param request contient le username et l'email du joueur
     * @return la représentation du joueur créé
     * @throws com.tournament.tournament_manager.exception.PlayerAlreadyExistsException si le username ou l'email est déjà utilisé
     */
    PlayerResponse createPlayer(CreatePlayerRequest request);
}
