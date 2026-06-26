package com.tournament.tournament_manager.domain.port.in.match;

import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.match.MatchResponse;

/**
 * Port entrant : cas d'utilisation pour enregistrer le résultat d'un match.
 */
public interface RecordMatchResultUseCase {

    /**
     * Enregistre le résultat d'un match et déclenche la chaîne Kafka.
     *
     * @param matchId identifiant du match
     * @param request contient l'identifiant du vainqueur
     * @return la représentation du match mis à jour
     */
    MatchResponse recordMatchResult(Long matchId, RecordMatchResultRequest request);
}