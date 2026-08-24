package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.domain.model.PurgeResult;
import com.tournament.tournament_manager.domain.port.in.maintenance.PurgeUseCase;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenche la purge périodique. Adapter d'entrée pur : aucune logique métier ici, tout
 * est délégué à {@link PurgeUseCase} (voir sa Javadoc et celle de
 * {@code application.maintenance.PurgeService} pour le détail de ce qui est purgé).
 *
 * <p>{@code @SchedulerLock} garantit qu'une seule instance exécute la purge à la fois en
 * déploiement multi-instances (voir {@code SchedulerLockConfig}) — sans ce verrou, le job
 * s'exécuterait N fois en parallèle à 2h du matin, une fois par instance.
 *
 * <p>Le job tourne tous les jours à 2h du matin. La durée de rétention
 * est configurable via {@code purge.retention-days} (défaut : 30 jours).
 */
@Slf4j
@Component
public class PurgeScheduler {

    private final PurgeUseCase purgeUseCase;

    @Value("${purge.retention-days:30}")
    private int retentionDays;

    public PurgeScheduler(PurgeUseCase purgeUseCase) {
        this.purgeUseCase = purgeUseCase;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "purgeDeletedEntities", lockAtLeastFor = "PT30S", lockAtMostFor = "PT15M")
    public void purgeDeletedEntities() {
        log.info("Démarrage de la purge (rétention : {} jours)", retentionDays);

        PurgeResult result = purgeUseCase.purgeDeletedEntities(retentionDays);

        log.info("Purge terminée : {} joueur(s) anonymisé(s) (historique conservé), {} joueur(s) "
                + "supprimé(s) physiquement (sans historique), {} tournoi(s), {} refresh token(s) "
                + "expiré(s) et {} événement(s) outbox publié(s) supprimés",
                result.anonymizedPlayers(), result.purgedPlayers(), result.purgedTournaments(),
                result.purgedRefreshTokens(), result.purgedOutboxEvents());
    }
}
