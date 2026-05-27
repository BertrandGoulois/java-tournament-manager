package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.dto.response.BracketResponse;

/**
 * Port entrant : cas d'utilisation pour consulter le bracket d'un tournoi.
 */
public interface GetBracketUseCase {

    /**
     * Retourne le bracket complet d'un tournoi, organisé par round.
     *
     * @param tournamentId identifiant du tournoi
     * @return le bracket du tournoi avec tous les matchs par round
     * @throws com.tournament.tournament_manager.exception.TournamentNotFoundException si le tournoi n'existe pas
     */
    BracketResponse getBracket(Long tournamentId);
}