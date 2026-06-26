package com.tournament.tournament_manager.domain.port.in.match;

import com.tournament.tournament_manager.dto.response.match.MatchCommentaryResponse;

/**
 * Port entrant : cas d'utilisation pour générer un commentaire de match via LLM.
 */
public interface GetMatchCommentaryUseCase {

    /**
     * Génère un commentaire narratif d'un match terminé via un LLM.
     *
     * @param matchId identifiant du match
     * @return le commentaire généré
     * @throws com.tournament.tournament_manager.exception.MatchNotFoundException si le match n'existe pas
     * @throws com.tournament.tournament_manager.exception.InvalidException si le match n'est pas terminé
     */
    MatchCommentaryResponse getMatchCommentary(Long matchId);
}