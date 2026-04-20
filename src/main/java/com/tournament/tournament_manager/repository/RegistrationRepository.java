package com.tournament.tournament_manager.repository;

import com.tournament.tournament_manager.domain.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    boolean existsByPlayerIdAndTournamentId(Long playerId, Long tournamentId);
    List<Registration> findByTournamentId(Long tournamentId);
}
