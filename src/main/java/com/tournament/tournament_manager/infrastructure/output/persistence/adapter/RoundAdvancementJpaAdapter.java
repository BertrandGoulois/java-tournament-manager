package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.enums.RoundAdvancementStatus;
import com.tournament.tournament_manager.domain.port.out.tournament.ClaimRoundAdvancementPort;
import com.tournament.tournament_manager.domain.port.out.tournament.ReleaseStaleRoundClaimsPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RoundAdvancementEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RoundAdvancementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Adapter JPA implémentant {@link ClaimRoundAdvancementPort} et
 * {@link ReleaseStaleRoundClaimsPort}.
 *
 * <p><b>Point 2.2 de la revue — le claim ne peut plus « brûler » un round.</b> L'écriture du
 * claim doit être commitée avant la transaction métier (sinon la contrainte d'unicité n'est
 * pas visible des concurrents), mais elle ne devient légitime qu'après elle. La version
 * précédente s'arrêtait au premier temps : un échec de la création des matchs annulait la
 * transaction métier en laissant le claim derrière lui. Le round devenait à jamais
 * inaccessible, et — le plus pernicieux — la redelivery Kafka tombait sur « round déjà
 * réclamé », journalisait un {@code warn} et <b>sortait en succès</b>. Le tournoi restait
 * bloqué sans qu'aucune erreur ne remonte nulle part.
 *
 * <p>Deux mécanismes referment la fenêtre :
 * <ol>
 *   <li>{@link #tryClaim} enregistre une {@link TransactionSynchronization} qui libère le
 *       claim si la transaction appelante est annulée. C'est un {@code afterCompletion} et
 *       non un {@code try/catch} : une transaction peut aussi échouer <b>au commit</b>, hors
 *       de portée de tout bloc catch placé dans la méthode — c'est exactement l'erreur
 *       corrigée au point 2.1 dans {@code EloService}, à ne pas reproduire ici.</li>
 *   <li>{@link #releaseStaleClaims} rattrape ce que le premier ne peut pas voir : un
 *       processus tué entre le commit du claim et la fin de la transaction métier.</li>
 * </ol>
 *
 * <p>Dans les deux cas, la libération suffit à débloquer : le retry Kafka (voir
 * {@code KafkaConfig}) rejouera l'événement et trouvera le round de nouveau réclamable.
 * Aucun redéclenchement manuel n'est nécessaire.
 */
@Slf4j
@Component
public class RoundAdvancementJpaAdapter implements ClaimRoundAdvancementPort, ReleaseStaleRoundClaimsPort {

    private final RoundAdvancementRepository roundAdvancementRepository;
    private final RoundClaimWriter claimWriter;

    public RoundAdvancementJpaAdapter(RoundAdvancementRepository roundAdvancementRepository,
                                      RoundClaimWriter claimWriter) {
        this.roundAdvancementRepository = roundAdvancementRepository;
        this.claimWriter = claimWriter;
    }

    @Override
    public boolean tryClaim(Long tournamentId, int round) {
        if (!claimWriter.insertPendingClaim(tournamentId, round)) {
            return false;
        }
        registerReleaseOnRollback(tournamentId, round);
        return true;
    }

    /**
     * Attache au cycle de vie de la transaction <b>appelante</b> la libération du claim en
     * cas d'annulation.
     *
     * <p>Noter que l'enregistrement a lieu ici et non dans {@link RoundClaimWriter} : à
     * l'intérieur d'une méthode {@code REQUIRES_NEW}, la transaction courante est la
     * transaction interne, qui commite immédiatement — la synchronisation s'y accrocherait
     * et se déclencherait aussitôt, sans jamais observer le sort de la transaction métier.
     */
    private void registerReleaseOnRollback(Long tournamentId, int round) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Appel hors transaction : rien à annuler, donc rien à libérer. Cas des tests
            // unitaires appelant l'adapter directement.
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    log.error("Transaction d'avancement de bracket annulée après réclamation du "
                                    + "round : libération du claim pour permettre une nouvelle tentative "
                                    + "[tournamentId={}, round={}]", tournamentId, round);
                    claimWriter.releasePendingClaim(tournamentId, round);
                }
            }
        });
    }

    /**
     * Rejoint la transaction métier ({@code @Transactional} sans propagation particulière) :
     * c'est tout l'intérêt. La confirmation du claim et la création des matchs doivent
     * réussir ou échouer ensemble, sans quoi on retrouverait un claim confirmé sans matchs.
     */
    @Override
    @Transactional
    public void markCompleted(Long tournamentId, int round) {
        int updated = roundAdvancementRepository.markDone(tournamentId, round);
        if (updated == 0) {
            log.warn("Aucun claim à confirmer, il a probablement été libéré entre-temps "
                    + "[tournamentId={}, round={}]", tournamentId, round);
        }
    }

    @Override
    @Transactional
    public List<StaleClaim> releaseStaleClaims(Duration olderThan) {
        Instant threshold = Instant.now().minus(olderThan);
        List<RoundAdvancementEntity> stale = roundAdvancementRepository
                .findByStatusAndCreatedAtBefore(RoundAdvancementStatus.PENDING, threshold);
        if (stale.isEmpty()) {
            return List.of();
        }
        roundAdvancementRepository.deleteAll(stale);
        return stale.stream()
                .map(e -> new StaleClaim(e.getTournamentId(), e.getRound()))
                .toList();
    }
}
