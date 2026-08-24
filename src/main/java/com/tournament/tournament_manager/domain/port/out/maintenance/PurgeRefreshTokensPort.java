package com.tournament.tournament_manager.domain.port.out.maintenance;

import java.time.Instant;

/**
 * Port sortant : purge des refresh tokens expirés. Indépendant de la politique de
 * rétention des soft deletes — un token expiré n'a plus aucun usage dès son expiration.
 */
public interface PurgeRefreshTokensPort {

    /**
     * Supprime les refresh tokens dont la date d'expiration est antérieure à {@code before}.
     *
     * @return le nombre de tokens supprimés
     */
    int deleteExpiredBefore(Instant before);
}
