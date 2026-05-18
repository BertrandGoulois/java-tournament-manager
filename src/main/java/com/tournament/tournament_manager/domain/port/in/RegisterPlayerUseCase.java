package com.tournament.tournament_manager.domain.port.in;

import com.tournament.tournament_manager.dto.request.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.RegistrationResponse;

/**
 * Port entrant : cas d'utilisation pour inscrire un joueur à un tournoi.
 */
public interface RegisterPlayerUseCase {

    /**
     * Inscrit un joueur à un tournoi.
     *
     * @param request contient l'identifiant du joueur et du tournoi
     * @return la représentation de l'inscription créée
     * @throws com.tournament.tournament_manager.exception.PlayerNotFoundException     si le joueur n'existe pas
     * @throws com.tournament.tournament_manager.exception.TournamentNotFoundException si le tournoi n'existe pas
     * @throws com.tournament.tournament_manager.exception.InvalidException            si le tournoi n'est pas ouvert
     * @throws com.tournament.tournament_manager.exception.InvalidException            si le joueur est déjà inscrit
     * @throws com.tournament.tournament_manager.exception.InvalidException            si le tournoi est complet
     */
    RegistrationResponse registerPlayer(CreateRegistrationRequest request);
}
