package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    @Query("SELECT m FROM MatchEntity m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.winner " +
            "WHERE m.tournament.id = :tournamentId")
    List<MatchEntity> findByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT m FROM MatchEntity m " +
            "LEFT JOIN FETCH m.player1 " +
            "LEFT JOIN FETCH m.player2 " +
            "LEFT JOIN FETCH m.winner " +
            "WHERE m.tournament.id = :tournamentId AND m.round = :round")
    List<MatchEntity> findByTournamentIdAndRound(@Param("tournamentId") Long tournamentId, @Param("round") int round);

    @Query("SELECT COUNT(m) FROM MatchEntity m "
            + "WHERE (m.player1.id = :playerId OR m.player2.id = :playerId) "
            + "AND m.status = com.tournament.tournament_manager.domain.model.enums.MatchStatus.FINISHED "
            + "AND m.player2 IS NOT NULL")
    long countFinishedRealMatchesByPlayer(@Param("playerId") Long playerId);

    @Query("SELECT COUNT(m) FROM MatchEntity m "
            + "WHERE m.winner.id = :playerId "
            + "AND m.status = com.tournament.tournament_manager.domain.model.enums.MatchStatus.FINISHED "
            + "AND m.player2 IS NOT NULL")
    long countRealWinsByPlayer(@Param("playerId") Long playerId);
}
