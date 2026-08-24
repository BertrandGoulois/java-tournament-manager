package com.tournament.tournament_manager.application.maintenance;

import com.tournament.tournament_manager.domain.model.PurgeResult;
import com.tournament.tournament_manager.domain.port.in.maintenance.PurgeUseCase;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgeOutboxEventsPort;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgePlayersPort;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgeRefreshTokensPort;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgeTournamentsPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Cas d'utilisation : purge périodique des entités soft-deleted, des refresh tokens
 * expirés et des événements outbox publiés.
 *
 * <p>Contrairement à sa version précédente, ce service ne dépend plus d'aucun repository
 * JPA — uniquement des ports sortants, comme tout autre service applicatif du projet. Le
 * déclenchement (le {@code @Scheduled}) vit désormais séparément dans
 * {@code infrastructure.input.scheduler.PurgeScheduler}, qui appelle ce use case sans en
 * porter la logique.
 *
 * <p>Pour les joueurs : ceux qui ont un historique (match ou inscription) sont
 * <b>anonymisés</b>, jamais supprimés physiquement — {@code matches}, {@code registrations}
 * et {@code elo_history} n'ont pas de {@code ON DELETE CASCADE} vers {@code players}, et
 * supprimer un joueur ayant joué reviendrait à amputer l'historique d'autres joueurs. Seuls
 * les joueurs soft-deleted sans aucun historique sont supprimés physiquement. Les tournois
 * soft-deleted, eux, sont toujours supprimés physiquement.
 *
 * <p>Les refresh tokens expirés et les événements outbox publiés sont purgés à chaque
 * exécution, indépendamment de {@code retentionDays} (une fois leur rôle rempli, ils n'ont
 * plus aucun usage, contrairement aux entités soft-deleted qu'on choisit de garder un
 * moment par sécurité).
 */
@Service
@Transactional
public class PurgeService implements PurgeUseCase {

    private final PurgePlayersPort purgePlayersPort;
    private final PurgeTournamentsPort purgeTournamentsPort;
    private final PurgeRefreshTokensPort purgeRefreshTokensPort;
    private final PurgeOutboxEventsPort purgeOutboxEventsPort;

    public PurgeService(PurgePlayersPort purgePlayersPort,
                        PurgeTournamentsPort purgeTournamentsPort,
                        PurgeRefreshTokensPort purgeRefreshTokensPort,
                        PurgeOutboxEventsPort purgeOutboxEventsPort) {
        this.purgePlayersPort = purgePlayersPort;
        this.purgeTournamentsPort = purgeTournamentsPort;
        this.purgeRefreshTokensPort = purgeRefreshTokensPort;
        this.purgeOutboxEventsPort = purgeOutboxEventsPort;
    }

    @Override
    public PurgeResult purgeDeletedEntities(int retentionDays) {
        Instant retentionLimit = Instant.now().minus(Duration.ofDays(retentionDays));

        int anonymizedPlayers = purgePlayersPort.anonymizeWithHistory(retentionLimit);
        int purgedPlayers = purgePlayersPort.purgeWithoutHistory(retentionLimit);
        int purgedTournaments = purgeTournamentsPort.purgeDeletedBefore(retentionLimit);
        int purgedRefreshTokens = purgeRefreshTokensPort.deleteExpiredBefore(Instant.now());
        int purgedOutboxEvents = purgeOutboxEventsPort.deletePublishedBefore(retentionLimit);

        return new PurgeResult(anonymizedPlayers, purgedPlayers, purgedTournaments,
                purgedRefreshTokens, purgedOutboxEvents);
    }
}
