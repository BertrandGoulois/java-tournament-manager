package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.domain.model.entities.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByUsername(String username);
    Optional<Player> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * Supprime physiquement les joueurs marqués comme supprimés
     * depuis plus de {@code retentionLimit}.
     *
     * <p>Utilise une requête native pour bypasser le {@code @SQLRestriction("deleted = false")}
     * qui empêche JPA de voir les entités soft-deleted.
     */
    @Modifying
    @Query(value = "DELETE FROM players WHERE deleted = true AND deleted_at < :retentionLimit", nativeQuery = true)
    int purgeDeletedBefore(LocalDateTime retentionLimit);
}