package com.tournament.tournament_manager.domain.port.in.match;

import com.tournament.tournament_manager.domain.model.MatchCommentary;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;

/**
 * Port entrant : cas d'utilisation pour générer un commentaire de match via LLM.
 */
public interface GetMatchCommentaryUseCase {

    /**
     * Retourne le commentaire narratif d'un match terminé.
     *
     * @param matchId identifiant du match
     * @return le commentaire
     * @throws MatchNotFoundException si le match n'existe pas
     * @throws InvalidException si le match n'est pas terminé
     */
    MatchCommentary getMatchCommentary(Long matchId);
}
