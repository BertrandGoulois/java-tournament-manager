package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.dto.response.tournament.StandingsResponse;

/**
 * Cas d'utilisation : consultation du classement d'un tournoi round-robin.
 */
public interface GetStandingsUseCase {

    /**
     * Retourne le classement complet d'un tournoi, trié par points décroissants.
     *
     * @param tournamentId identifiant du tournoi
     * @return le classement du tournoi
     */
    StandingsResponse getStandings(Long tournamentId);
}