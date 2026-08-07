package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RoundAdvancementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoundAdvancementRepository extends JpaRepository<RoundAdvancementEntity, Long> {
}
