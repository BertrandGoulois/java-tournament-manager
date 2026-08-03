package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.domain.model.entities.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Verrouille et retourne un lot d'événements non publiés, par ordre de création.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED} rend ce polling sûr même avec plusieurs instances
     * de l'application tournant en parallèle : chaque instance saute les lignes déjà
     * verrouillées par une autre plutôt que d'attendre ou de les traiter en double.
     * Ce n'est pas exprimable en JPQL portable — requête native, mais le projet ne cible
     * que PostgreSQL (voir docker-compose.yml).
     */
    @Query(value = "SELECT * FROM outbox_events "
            + "WHERE published_at IS NULL "
            + "ORDER BY id "
            + "LIMIT :batchSize "
            + "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> lockNextUnpublishedBatch(@Param("batchSize") int batchSize);

    /**
     * Supprime physiquement les événements déjà publiés depuis plus de {@code cutoff}.
     * Les événements non publiés ne sont jamais purgés, quel que soit leur âge — un
     * événement bloqué signale un problème (Kafka indisponible durablement) à corriger,
     * pas à faire disparaître silencieusement.
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.publishedAt IS NOT NULL AND e.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") LocalDateTime cutoff);
}
