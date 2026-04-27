package com.tournament.tournament_manager.repository;

import com.tournament.tournament_manager.domain.model.entities.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentId(Long tournamentId);
    List<Match> findByTournamentIdAndRound(Long tournamentId, int round);
    long countByPlayer1IdOrPlayer2Id(Long player1Id, Long player2Id);
    long countByWinnerId(Long winnerId);
}
