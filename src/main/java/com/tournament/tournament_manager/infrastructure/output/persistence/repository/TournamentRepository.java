package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TournamentRepository extends JpaRepository<TournamentEntity, Long> {
    boolean existsByName(String name);

    @Modifying
    @Query(value = "DELETE FROM tournaments WHERE deleted = true AND deleted_at < :retentionLimit", nativeQuery = true)
    int purgeDeletedBefore(Instant retentionLimit);
}
