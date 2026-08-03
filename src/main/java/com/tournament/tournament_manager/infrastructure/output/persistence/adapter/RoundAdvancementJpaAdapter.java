package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.entities.RoundAdvancement;
import com.tournament.tournament_manager.domain.port.out.tournament.ClaimRoundAdvancementPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RoundAdvancementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter JPA implémentant {@link ClaimRoundAdvancementPort}.
 *
 * <p>{@code Propagation.REQUIRES_NEW} est essentiel ici : la réclamation doit s'exécuter
 * dans sa propre transaction, indépendante de celle de {@code AdvanceBracketService}. Sans
 * ça, un conflit de contrainte d'unicité mettrait la transaction appelante en échec
 * irrécupérable (comportement standard PostgreSQL après une erreur SQL non rattrapée par un
 * savepoint), empêchant de simplement "retourner false" et continuer normalement.
 */
@Slf4j
@Component
public class RoundAdvancementJpaAdapter implements ClaimRoundAdvancementPort {

    private final RoundAdvancementRepository roundAdvancementRepository;

    public RoundAdvancementJpaAdapter(RoundAdvancementRepository roundAdvancementRepository) {
        this.roundAdvancementRepository = roundAdvancementRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryClaim(Long tournamentId, int round) {
        RoundAdvancement marker = new RoundAdvancement();
        marker.setTournamentId(tournamentId);
        marker.setRound(round);
        try {
            roundAdvancementRepository.saveAndFlush(marker);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Round déjà réclamé, réclamation ignorée [tournamentId={}, round={}]",
                    tournamentId, round);
            return false;
        }
    }
}
