package com.tournament.tournament_manager.domain.port.out.tournament;

import java.time.Duration;
import java.util.List;

/**
 * Port sortant : libère les claims de round restés non confirmés au-delà d'un délai.
 *
 * <p>Filet de sécurité de dernier recours pour le point 2.2. Le cas nominal est déjà couvert
 * par la libération automatique sur rollback ({@code ClaimRoundAdvancementPort.tryClaim}) ;
 * ce port ne rattrape que ce que celle-ci ne peut pas voir — typiquement un processus tué
 * entre le commit du claim et la fin de la transaction métier.
 */
public interface ReleaseStaleRoundClaimsPort {

    /**
     * Libère les claims non confirmés créés il y a plus de {@code olderThan}.
     *
     * @return description des claims libérés, pour journalisation et supervision. Une liste
     *         non vide est <b>toujours</b> anormale : elle signale qu'une transaction
     *         d'avancement de bracket est morte sans se nettoyer.
     */
    List<StaleClaim> releaseStaleClaims(Duration olderThan);

    /** Claim libéré, identifié par le round qu'il bloquait. */
    record StaleClaim(Long tournamentId, int round) {}
}
