package com.tournament.tournament_manager.repository;

import com.tournament.tournament_manager.domain.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    boolean existsByName(String name);
}
