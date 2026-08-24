package com.tournament.tournament_manager.domain.port.out.maintenance;

import java.time.Instant;

/**
 * Port sortant : purge des tournois soft-deleted, toujours supprimés physiquement
 * (contrairement aux joueurs, un tournoi n'a pas d'historique appartenant à un tiers
 * à préserver).
 */
public interface PurgeTournamentsPort {

    /**
     * Supprime physiquement les tournois soft-deleted depuis plus de {@code retentionLimit}.
     *
     * @return le nombre de tournois supprimés
     */
    int purgeDeletedBefore(Instant retentionLimit);
}
