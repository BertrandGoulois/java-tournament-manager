package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.infrastructure.output.persistence.repository.OutboxEventRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RefreshTokenRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service de purge périodique des entités soft-deleted, des refresh tokens expirés, et
 * des événements outbox déjà publiés.
 *
 * <p>Les joueurs et tournois supprimés (soft delete) sont conservés en base
 * pendant une durée configurable ({@code purge.retention-days}), puis traités
 * par ce job planifié. Pour les joueurs : ceux qui ont un historique (match ou
 * inscription) sont <b>anonymisés</b>, jamais supprimés physiquement — {@code matches},
 * {@code registrations} et {@code elo_history} n'ont pas de {@code ON DELETE CASCADE} vers
 * {@code players}, et supprimer un joueur ayant joué reviendrait à amputer l'historique
 * d'autres joueurs. Seuls les joueurs soft-deleted sans aucun historique sont supprimés
 * physiquement. Les tournois soft-deleted, eux, sont toujours supprimés physiquement
 * (voir {@code TournamentRepository.purgeDeletedBefore}).
 *
 * <p>Les refresh tokens expirés et les événements outbox publiés sont purgés à chaque
 * exécution, indépendamment de {@code purge.retention-days} (une fois leur rôle rempli,
 * ils n'ont plus aucun usage, contrairement aux entités soft-deleted qu'on choisit de
 * garder un moment par sécurité).
 *
 * <p>Le job tourne tous les jours à 2h du matin. La durée de rétention
 * est configurable via {@code purge.retention-days} (défaut : 30 jours).
 */
@Slf4j
@Service
public class PurgeService {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${purge.retention-days:30}")
    private int retentionDays;

    public PurgeService(PlayerRepository playerRepository,
                        TournamentRepository tournamentRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        OutboxEventRepository outboxEventRepository) {
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * Traite les entités soft-deleted depuis plus de {@code purge.retention-days} jours
     * (anonymise les joueurs avec historique, supprime physiquement les autres et les
     * tournois), purge les refresh tokens expirés, et les événements outbox déjà publiés.
     * Exécuté automatiquement tous les jours à 2h du matin.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeDeletedEntities() {
        LocalDateTime retentionLimit = LocalDateTime.now().minusDays(retentionDays);
        log.info("Démarrage de la purge des entités supprimées avant le {}", retentionLimit);

        int anonymizedPlayers = playerRepository.anonymizeWithHistory(retentionLimit);
        int purgedPlayers = playerRepository.purgeDeletedWithoutHistory(retentionLimit);
        int purgedTournaments = tournamentRepository.purgeDeletedBefore(retentionLimit);
        int purgedRefreshTokens = refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now());
        int purgedOutboxEvents = outboxEventRepository.deletePublishedBefore(retentionLimit);

        log.info("Purge terminée : {} joueur(s) anonymisé(s) (historique conservé), {} joueur(s) "
                + "supprimé(s) physiquement (sans historique), {} tournoi(s), {} refresh token(s) "
                + "expiré(s) et {} événement(s) outbox publié(s) supprimés",
                anonymizedPlayers, purgedPlayers, purgedTournaments, purgedRefreshTokens, purgedOutboxEvents);
    }
}