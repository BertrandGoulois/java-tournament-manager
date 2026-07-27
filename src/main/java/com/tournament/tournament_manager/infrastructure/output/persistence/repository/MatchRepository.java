package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.domain.model.entities.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.winner " +
            "WHERE m.tournament.id = :tournamentId")
    List<Match> findByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT m FROM Match m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.winner " +
            "WHERE m.tournament.id = :tournamentId AND m.round = :round")
    List<Match> findByTournamentIdAndRound(@Param("tournamentId") Long tournamentId, @Param("round") int round);

    long countByPlayer1IdOrPlayer2Id(Long player1Id, Long player2Id);
    long countByWinnerId(Long winnerId);
}
