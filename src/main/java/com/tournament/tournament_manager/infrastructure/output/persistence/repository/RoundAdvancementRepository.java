package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.domain.model.enums.RoundAdvancementStatus;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RoundAdvancementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RoundAdvancementRepository extends JpaRepository<RoundAdvancementEntity, Long> {

    /**
     * Marque un claim comme confirmé. Requête de modification directe plutôt que
     * chargement + setter : cet appel s'exécute dans la transaction métier, et n'a besoin
     * d'aucun état de l'entité.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RoundAdvancementEntity ra
            SET ra.status = com.tournament.tournament_manager.domain.model.enums.RoundAdvancementStatus.DONE
            WHERE ra.tournamentId = :tournamentId AND ra.round = :round
            """)
    int markDone(@Param("tournamentId") Long tournamentId, @Param("round") int round);

    /**
     * Supprime un claim, libérant le round pour une nouvelle tentative. Utilisé sur
     * rollback de la transaction métier et par le job de réconciliation.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM RoundAdvancementEntity ra
            WHERE ra.tournamentId = :tournamentId AND ra.round = :round
              AND ra.status = com.tournament.tournament_manager.domain.model.enums.RoundAdvancementStatus.PENDING
            """)
    int deletePendingClaim(@Param("tournamentId") Long tournamentId, @Param("round") int round);

    /**
     * Claims restés PENDING au-delà du seuil : la transaction métier correspondante a été
     * annulée (ou le processus est mort) sans que le claim soit libéré.
     *
     * <p>La condition sur {@code status} est essentielle : sans elle, la requête libérerait
     * aussi des rounds réellement créés, autorisant leur recréation avec un appariement
     * différent — soit précisément le dédoublement que le claim existe pour empêcher.
     */
    List<RoundAdvancementEntity> findByStatusAndCreatedAtBefore(RoundAdvancementStatus status, Instant threshold);
}
