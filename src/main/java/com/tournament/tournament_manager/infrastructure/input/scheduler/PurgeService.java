package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service de purge périodique des entités soft-deleted.
 *
 * <p>Les joueurs et tournois supprimés (soft delete) sont conservés en base
 * pendant une durée configurable ({@code purge.retention-days}), puis
 * supprimés physiquement par ce job planifié.
 *
 * <p>Le job tourne tous les jours à 2h du matin. La durée de rétention
 * est configurable via {@code purge.retention-days} (défaut : 30 jours).
 */
@Slf4j
@Service
public class PurgeService {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;

    @Value("${purge.retention-days:30}")
    private int retentionDays;

    public PurgeService(PlayerRepository playerRepository,
                        TournamentRepository tournamentRepository) {
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
    }

    /**
     * Purge les entités soft-deleted depuis plus de {@code purge.retention-days} jours.
     * Exécuté automatiquement tous les jours à 2h du matin.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeDeletedEntities() {
        LocalDateTime retentionLimit = LocalDateTime.now().minusDays(retentionDays);
        log.info("Démarrage de la purge des entités supprimées avant le {}", retentionLimit);

        int purgedPlayers = playerRepository.purgeDeletedBefore(retentionLimit);
        int purgedTournaments = tournamentRepository.purgeDeletedBefore(retentionLimit);

        log.info("Purge terminée : {} joueur(s) et {} tournoi(s) supprimés physiquement",
                purgedPlayers, purgedTournaments);
    }
}