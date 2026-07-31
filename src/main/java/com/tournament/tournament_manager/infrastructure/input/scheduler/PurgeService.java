package com.tournament.tournament_manager.infrastructure.input.scheduler;

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
 * Service de purge périodique des entités soft-deleted et des refresh tokens expirés.
 *
 * <p>Les joueurs et tournois supprimés (soft delete) sont conservés en base
 * pendant une durée configurable ({@code purge.retention-days}), puis
 * supprimés physiquement par ce job planifié. Les refresh tokens expirés
 * sont purgés à chaque exécution, indépendamment de {@code purge.retention-days}
 * (une fois expiré, un refresh token n'a plus aucun usage, contrairement aux
 * entités soft-deleted qu'on choisit de garder un moment par sécurité).
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

    @Value("${purge.retention-days:30}")
    private int retentionDays;

    public PurgeService(PlayerRepository playerRepository,
                        TournamentRepository tournamentRepository,
                        RefreshTokenRepository refreshTokenRepository) {
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Purge les entités soft-deleted depuis plus de {@code purge.retention-days} jours,
     * ainsi que les refresh tokens expirés. Exécuté automatiquement tous les jours à 2h du matin.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeDeletedEntities() {
        LocalDateTime retentionLimit = LocalDateTime.now().minusDays(retentionDays);
        log.info("Démarrage de la purge des entités supprimées avant le {}", retentionLimit);

        int purgedPlayers = playerRepository.purgeDeletedBefore(retentionLimit);
        int purgedTournaments = tournamentRepository.purgeDeletedBefore(retentionLimit);
        int purgedRefreshTokens = refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now());

        log.info("Purge terminée : {} joueur(s), {} tournoi(s) et {} refresh token(s) expiré(s) supprimés physiquement",
                purgedPlayers, purgedTournaments, purgedRefreshTokens);
    }
}