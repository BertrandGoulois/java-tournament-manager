package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.TournamentResponse;

/**
 * Port entrant : cas d'utilisation pour créer un tournoi.
 */
public interface CreateTournamentUseCase {

    /**
     * Crée un nouveau tournoi au statut {@code OPEN}.
     *
     * @param request contient le nom et le nombre maximum de joueurs
     * @return la représentation du tournoi créé
     * @throws com.tournament.tournament_manager.exception.TournamentAlreadyExistsException si le nom est déjà utilisé
     * @throws com.tournament.tournament_manager.exception.InvalidTournamentException si {@code maxPlayers} n'est pas une puissance de 2
     */
    TournamentResponse createTournament(CreateTournamentRequest request);
}