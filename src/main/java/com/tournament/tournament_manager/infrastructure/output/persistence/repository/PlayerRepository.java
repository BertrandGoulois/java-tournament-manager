package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    Optional<PlayerEntity> findByUsername(String username);
    Optional<PlayerEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Modifying
    @Query(value = """
            UPDATE players
            SET username = 'utilisateur-supprime-' || id,
                email = 'supprime-' || id || '@anonymise.invalid',
                anonymized_at = NOW()
            WHERE deleted = true
              AND deleted_at < :retentionLimit
              AND anonymized_at IS NULL
              AND (
                EXISTS (SELECT 1 FROM matches m
                        WHERE m.player1_id = players.id OR m.player2_id = players.id OR m.winner_id = players.id)
                OR EXISTS (SELECT 1 FROM registrations r WHERE r.player_id = players.id)
              )
            """, nativeQuery = true)
    int anonymizeWithHistory(Instant retentionLimit);

    @Modifying
    @Query(value = """
            DELETE FROM players
            WHERE deleted = true
              AND deleted_at < :retentionLimit
              AND NOT EXISTS (SELECT 1 FROM matches m
                              WHERE m.player1_id = players.id OR m.player2_id = players.id OR m.winner_id = players.id)
              AND NOT EXISTS (SELECT 1 FROM registrations r WHERE r.player_id = players.id)
            """, nativeQuery = true)
    int purgeDeletedWithoutHistory(Instant retentionLimit);
}
