package com.tournament.tournament_manager.repository;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EloHistoryRepository extends JpaRepository<EloHistory, Long> {
    List<EloHistory> findByPlayerIdOrderByCreatedAtDesc(Long playerId);
}
