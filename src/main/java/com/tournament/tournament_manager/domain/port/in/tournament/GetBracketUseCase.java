package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.domain.model.Bracket;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;

/**
 * Port entrant : cas d'utilisation pour consulter le bracket d'un tournoi.
 */
public interface GetBracketUseCase {

    /**
     * Retourne le bracket complet d'un tournoi, organisé par round.
     *
     * @param tournamentId identifiant du tournoi
     * @return le bracket du tournoi avec tous les matchs par round
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     */
    Bracket getBracket(Long tournamentId);
}
