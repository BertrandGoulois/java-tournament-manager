package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RoundAdvancementEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.RoundAdvancementMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RoundAdvancementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Écritures de claim devant s'exécuter dans leur <b>propre</b> transaction, indépendamment
 * de la transaction métier en cours.
 *
 * <p>Composant séparé de {@link RoundAdvancementJpaAdapter} pour une raison précise et non
 * cosmétique : {@code @Transactional} passe par un proxy Spring, et un appel d'une méthode
 * d'un bean vers une autre méthode du <i>même</i> bean court-circuite ce proxy. Si ces
 * méthodes vivaient dans l'adapter, leur {@code REQUIRES_NEW} serait silencieusement ignoré
 * quand l'adapter les appelle — elles rejoindraient la transaction métier, et toute la
 * mécanique de claim indépendant s'effondrerait sans le moindre message d'erreur.
 */
@Slf4j
@Component
class RoundClaimWriter {

    private final RoundAdvancementRepository repository;
    private final RoundAdvancementMapper mapper;

    RoundClaimWriter(RoundAdvancementRepository repository, RoundAdvancementMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Insère un claim non confirmé, en transaction indépendante.
     *
     * <p>{@code REQUIRES_NEW} est essentiel : la réclamation doit être commitée avant la
     * transaction métier pour que la contrainte d'unicité soit visible des concurrents. Sans
     * ça, un conflit mettrait la transaction appelante en échec irrécupérable (comportement
     * PostgreSQL standard après une erreur SQL non rattrapée par un savepoint), au lieu de
     * simplement renvoyer {@code false} et laisser l'appelant continuer.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean insertPendingClaim(Long tournamentId, int round) {
        RoundAdvancementEntity marker = mapper.toNewEntity(tournamentId, round);
        try {
            repository.saveAndFlush(marker);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Round déjà réclamé, réclamation ignorée [tournamentId={}, round={}]",
                    tournamentId, round);
            return false;
        }
    }

    /**
     * Supprime un claim non confirmé, en transaction indépendante.
     *
     * <p>Appelé depuis {@code afterCompletion}, donc alors que la transaction métier est
     * déjà terminée et qu'aucune transaction n'est active : {@code REQUIRES_NEW} en ouvre
     * une propre. La condition {@code status = PENDING} de la requête protège d'un cas
     * limite — si le claim a entre-temps été confirmé par une autre exécution, on ne le
     * supprime pas.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void releasePendingClaim(Long tournamentId, int round) {
        int deleted = repository.deletePendingClaim(tournamentId, round);
        if (deleted > 0) {
            log.warn("Claim de round libéré après annulation de la transaction métier, le round "
                    + "reste créable [tournamentId={}, round={}]", tournamentId, round);
        }
    }
}
