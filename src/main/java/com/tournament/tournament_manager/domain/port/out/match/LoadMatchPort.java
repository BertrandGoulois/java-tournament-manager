package com.tournament.tournament_manager.domain.port.out.match;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;

/**
 * Port sortant : chargement d'un match depuis la persistance.
 */
public interface LoadMatchPort {

    /**
     * Charge un match par son identifiant.
     *
     * @param id identifiant du match
     * @return le match correspondant
     * @throws MatchNotFoundException si le match n'existe pas
     */
    Match loadMatch(Long id);
}