package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query(value = "SELECT * FROM outbox_events "
            + "WHERE published_at IS NULL "
            + "ORDER BY id "
            + "LIMIT :batchSize "
            + "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEventEntity> lockNextUnpublishedBatch(@Param("batchSize") int batchSize);

    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.publishedAt IS NOT NULL AND e.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
