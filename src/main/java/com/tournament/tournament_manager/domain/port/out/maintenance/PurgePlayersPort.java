package com.tournament.tournament_manager.domain.port.out.maintenance;

import java.time.Instant;

/**
 * Port sortant : purge des joueurs soft-deleted.
 *
 * <p>Deux opérations distinctes, jamais l'une sans l'autre dans l'ordre attendu par
 * {@code PurgeService} : un joueur avec historique (match ou inscription) est anonymisé,
 * jamais supprimé physiquement — voir {@code PlayerRepository.anonymizeWithHistory} pour
 * le détail de cette distinction.
 */
public interface PurgePlayersPort {

    /**
     * Anonymise les joueurs soft-deleted depuis plus de {@code retentionLimit} qui ont un
     * historique (match ou inscription) : leurs données personnelles sont écrasées, la
     * ligne est conservée.
     *
     * @return le nombre de joueurs anonymisés
     */
    int anonymizeWithHistory(Instant retentionLimit);

    /**
     * Supprime physiquement les joueurs soft-deleted depuis plus de {@code retentionLimit}
     * qui n'ont aucun historique.
     *
     * @return le nombre de joueurs supprimés
     */
    int purgeWithoutHistory(Instant retentionLimit);
}
