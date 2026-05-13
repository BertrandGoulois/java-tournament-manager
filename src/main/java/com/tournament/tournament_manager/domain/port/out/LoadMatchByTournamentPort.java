package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Match;

import java.util.List;

/**
 * Port sortant : chargement des matchs d'un tournoi par round.
 */
public interface LoadMatchByTournamentPort {

    /**
     * Charge tous les matchs d'un tournoi pour un round donné.
     *
     * @param tournamentId identifiant du tournoi
     * @param round        numéro du round
     * @return liste des matchs du round
     */
    List<Match> loadByTournamentIdAndRound(Long tournamentId, int round);
}
