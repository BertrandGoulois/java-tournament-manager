package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import com.tournament.tournament_manager.exception.domain.InvalidTournamentException;
import com.tournament.tournament_manager.exception.domain.TournamentAlreadyExistsException;

/**
 * Port entrant : cas d'utilisation pour créer un tournoi.
 */
public interface CreateTournamentUseCase {

    /**
     * Crée un nouveau tournoi au statut {@code OPEN}.
     *
     * @param request contient le nom et le nombre maximum de joueurs
     * @return la représentation du tournoi créé
     * @throws TournamentAlreadyExistsException si le nom est déjà utilisé
     * @throws InvalidTournamentException si {@code maxPlayers} n'est pas une puissance de 2
     */
    TournamentResponse createTournament(CreateTournamentRequest request);
}