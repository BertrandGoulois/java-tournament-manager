package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Match;

/**
 * Port sortant : chargement d'un match depuis la persistance.
 */
public interface LoadMatchPort {

    /**
     * Charge un match par son identifiant.
     *
     * @param id identifiant du match
     * @return le match correspondant
     * @throws com.tournament.tournament_manager.exception.MatchNotFoundException si le match n'existe pas
     */
    Match loadMatch(Long id);
}