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

    /**
     * Compte les matchs réellement joués par un joueur : {@code FINISHED} et à deux joueurs
     * réels (exclut les {@code PENDING} — un match programmé n'est pas "joué" — et les byes,
     * qui n'opposent qu'un seul joueur réel).
     */
    @Query("SELECT COUNT(m) FROM Match m "
            + "WHERE (m.player1.id = :playerId OR m.player2.id = :playerId) "
            + "AND m.status = com.tournament.tournament_manager.domain.model.enums.MatchStatus.FINISHED "
            + "AND m.player2 IS NOT NULL")
    long countFinishedRealMatchesByPlayer(@Param("playerId") Long playerId);

    /**
     * Compte les victoires réelles d'un joueur : exclut les byes (le vainqueur d'un bye
     * n'a battu personne, {@code player2} y est {@code null}).
     */
    @Query("SELECT COUNT(m) FROM Match m "
            + "WHERE m.winner.id = :playerId "
            + "AND m.status = com.tournament.tournament_manager.domain.model.enums.MatchStatus.FINISHED "
            + "AND m.player2 IS NOT NULL")
    long countRealWinsByPlayer(@Param("playerId") Long playerId);
}
