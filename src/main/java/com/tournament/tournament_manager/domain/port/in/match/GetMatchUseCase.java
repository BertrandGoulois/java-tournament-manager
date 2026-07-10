package com.tournament.tournament_manager.domain.port.in.match;

import com.tournament.tournament_manager.dto.response.match.MatchResponse;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;

/**
 * Port entrant : cas d'utilisation pour consulter un match.
 */
public interface GetMatchUseCase {

    /**
     * Retourne un match par son identifiant.
     *
     * @param id identifiant du match
     * @return la représentation du match
     * @throws MatchNotFoundException si le match n'existe pas
     */
    MatchResponse getMatchById(Long id);
}