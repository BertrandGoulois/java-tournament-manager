package com.tournament.tournament_manager.domain.port.in.maintenance;

import com.tournament.tournament_manager.domain.model.PurgeResult;

/**
 * Port entrant : cas d'utilisation de la purge périodique des entités soft-deleted, des
 * refresh tokens expirés et des événements outbox publiés.
 *
 * <p>Déclenché par {@code PurgeScheduler} (un {@code @Scheduled}, dans
 * {@code infrastructure.input.scheduler}) — ce port sépare le déclenchement technique
 * (le "quand") de la logique métier de purge elle-même (le "quoi"), exactement comme un
 * contrôleur REST déclenche un use case sans en porter la logique.
 */
public interface PurgeUseCase {

    /**
     * Exécute une passe de purge.
     *
     * @param retentionDays nombre de jours de rétention avant qu'une entité soft-deleted
     *                       ne devienne éligible à la purge
     * @return le détail de ce qui a été traité
     */
    PurgeResult purgeDeletedEntities(int retentionDays);
}
