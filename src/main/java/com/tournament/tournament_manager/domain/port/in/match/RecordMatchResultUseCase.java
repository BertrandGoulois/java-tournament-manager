package com.tournament.tournament_manager.domain.port.in.match;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.RecordMatchResultCommand;

/**
 * Port entrant : cas d'utilisation pour enregistrer le résultat d'un match.
 */
public interface RecordMatchResultUseCase {

    /**
     * Enregistre le résultat d'un match et déclenche la chaîne Kafka.
     *
     * @param matchId identifiant du match
     * @param command contient l'identifiant du vainqueur
     * @return le match mis à jour
     */
    Match recordMatchResult(Long matchId, RecordMatchResultCommand command);
}
