package com.tournament.tournament_manager.domain.port.in;

import com.tournament.tournament_manager.dto.response.MatchResponse;

/**
 * Port entrant : cas d'utilisation pour consulter un match.
 */
public interface GetMatchUseCase {

    /**
     * Retourne un match par son identifiant.
     *
     * @param id identifiant du match
     * @return la représentation du match
     * @throws com.tournament.tournament_manager.exception.MatchNotFoundException si le match n'existe pas
     */
    MatchResponse getMatchById(Long id);
}