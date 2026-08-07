package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.EloHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EloHistoryRepository extends JpaRepository<EloHistoryEntity, Long> {
    List<EloHistoryEntity> findByPlayerIdOrderByCreatedAtDesc(Long playerId);
    boolean existsByMatchId(Long matchId);
}
