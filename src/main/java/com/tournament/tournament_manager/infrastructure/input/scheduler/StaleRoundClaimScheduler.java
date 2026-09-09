package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.domain.port.out.tournament.ReleaseStaleRoundClaimsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Libère périodiquement les claims de round restés non confirmés (point 2.2 de la revue).
 *
 * <p>Filet de dernier recours, pas mécanisme principal. Le cas nominal — la transaction
 * d'avancement échoue — est déjà traité à chaud par {@code RoundAdvancementJpaAdapter}, qui
 * libère le claim dès l'annulation. Ce job ne rattrape que ce qu'une synchronisation de
 * transaction ne peut pas voir : un processus tué entre le commit du claim et la fin de la
 * transaction métier. C'est rare, mais sans lui ce scénario laisse un tournoi bloqué pour
 * toujours, ce qui est précisément le défaut qu'on cherche à éliminer.
 *
 * <p><b>Toute libération est anormale.</b> D'où le {@code log.error} et le compteur
 * {@code round.claims.stale.released} : ce job ne doit jamais rien trouver en régime normal,
 * et le jour où il trouve quelque chose, c'est le symptôme d'un incident (kill -9, OOM,
 * nœud perdu) qui mérite qu'on aille voir. Un job de réparation silencieux transformerait un
 * bug en bruit de fond.
 *
 * <p>Aucun redéclenchement n'est nécessaire après libération : l'événement Kafka
 * correspondant n'a pas vu son offset commité (le processus est mort avant), il sera donc
 * redélivré et trouvera le round de nouveau réclamable.
 *
 * <p>Le seuil {@code round-claims.stale-after} doit rester nettement supérieur à la durée
 * d'une transaction d'avancement (quelques dizaines de millisecondes) : trop court, il
 * libérerait un claim d'une transaction encore en cours et autoriserait une création
 * concurrente du même round — soit exactement ce que le claim empêche.
 */
@Slf4j
@Component
public class StaleRoundClaimScheduler {

    private final ReleaseStaleRoundClaimsPort releaseStaleRoundClaimsPort;
    private final Counter staleClaimsReleasedCounter;

    @Value("${round-claims.stale-after:PT10M}")
    private Duration staleAfter;

    public StaleRoundClaimScheduler(ReleaseStaleRoundClaimsPort releaseStaleRoundClaimsPort,
                                    MeterRegistry meterRegistry) {
        this.releaseStaleRoundClaimsPort = releaseStaleRoundClaimsPort;
        this.staleClaimsReleasedCounter = Counter.builder("round.claims.stale.released")
                .description("Claims de round non confirmés libérés par le job de réconciliation "
                        + "(toute valeur non nulle signale un incident)")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${round-claims.reconciliation-interval:PT5M}")
    @SchedulerLock(name = "releaseStaleRoundClaims", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    public void releaseStaleRoundClaims() {
        List<ReleaseStaleRoundClaimsPort.StaleClaim> released =
                releaseStaleRoundClaimsPort.releaseStaleClaims(staleAfter);

        if (released.isEmpty()) {
            log.debug("Aucun claim de round non confirmé à libérer");
            return;
        }

        staleClaimsReleasedCounter.increment(released.size());
        released.forEach(claim -> log.error(
                "Claim de round non confirmé depuis plus de {}, libéré : une transaction "
                        + "d'avancement de bracket est morte sans se nettoyer "
                        + "[tournamentId={}, round={}]",
                staleAfter, claim.tournamentId(), claim.round()));
    }
}
