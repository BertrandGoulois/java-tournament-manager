package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByToken(String token);
    void deleteByUsername(String username);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.expiryDate < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
