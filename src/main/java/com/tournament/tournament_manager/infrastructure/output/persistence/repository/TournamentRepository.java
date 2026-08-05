package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    boolean existsByName(String name);

    /**
     * Supprime physiquement les tournois marqués comme supprimés
     * depuis plus de {@code retentionLimit}.
     *
     * <p>Utilise une requête native pour bypasser le {@code @SQLRestriction("deleted = false")}
     * qui empêche JPA de voir les entités soft-deleted.
     */
    @Modifying
    @Query(value = "DELETE FROM tournaments WHERE deleted = true AND deleted_at < :retentionLimit", nativeQuery = true)
    int purgeDeletedBefore(Instant retentionLimit);
}