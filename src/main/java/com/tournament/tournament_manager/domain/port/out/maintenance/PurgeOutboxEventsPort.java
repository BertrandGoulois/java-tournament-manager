package com.tournament.tournament_manager.domain.port.out.maintenance;

import java.time.Instant;

/**
 * Port sortant : purge des événements outbox déjà publiés. Les événements non publiés ne
 * sont jamais concernés — un événement bloqué signale un problème à corriger, pas à faire
 * disparaître silencieusement.
 */
public interface PurgeOutboxEventsPort {

    /**
     * Supprime les événements outbox publiés avant {@code before}.
     *
     * @return le nombre d'événements supprimés
     */
    int deletePublishedBefore(Instant before);
}
