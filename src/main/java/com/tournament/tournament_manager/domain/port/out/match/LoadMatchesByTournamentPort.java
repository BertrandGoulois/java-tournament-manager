package com.tournament.tournament_manager.domain.port.out.match;

import com.tournament.tournament_manager.domain.model.Match;

import java.util.List;

/**
 * Port sortant : chargement de tous les matchs d'un tournoi.
 */
public interface LoadMatchesByTournamentPort {

    /**
     * Charge tous les matchs d'un tournoi.
     *
     * @param tournamentId identifiant du tournoi
     * @return liste de tous les matchs du tournoi
     */
    List<Match> loadByTournamentId(Long tournamentId);
}