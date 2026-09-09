package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <p><b>§4 de la revue.</b> Depuis le passage des {@code @ManyToOne} de {@code MatchEntity}
 * en {@code FetchType.LAZY}, <b>toute</b> méthode renvoyant des {@code MatchEntity} destinés
 * à {@code MatchMapper.toDomain} doit précharger les quatre associations : le mapper les
 * déréférence sans condition, et une association LAZY non préchargée devient un SELECT par
 * relation et par ligne. Ajouter une requête sans {@code JOIN FETCH} ici, c'est réintroduire
 * un N+1 silencieux.
 */
@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    /**
     * Chargement unitaire avec associations, pour le chemin {@code LoadMatchPort.loadMatch}.
     *
     * <p>Remplace le {@code findById} hérité de {@code JpaRepository} : celui-ci ne connaît
     * aucun {@code JOIN FETCH}, et produisait donc quatre SELECT supplémentaires par match
     * une fois les associations passées en LAZY — là où l'EAGER précédent faisait une seule
     * requête jointe. Le gain du LAZY n'est réel que porté par une requête comme celle-ci.
     */
    @Query("""
            SELECT m FROM MatchEntity m
            LEFT JOIN FETCH m.tournament
            LEFT JOIN FETCH m.player1
            LEFT JOIN FETCH m.player2
            LEFT JOIN FETCH m.winner
            WHERE m.id = :id
            """)
    Optional<MatchEntity> findByIdWithAssociations(@Param("id") Long id);

    @Query("""
            SELECT m FROM MatchEntity m
            LEFT JOIN FETCH m.tournament
            LEFT JOIN FETCH m.player1
            LEFT JOIN FETCH m.player2
            LEFT JOIN FETCH m.winner
            WHERE m.tournament.id = :tournamentId
            """)
    List<MatchEntity> findByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("""
            SELECT m FROM MatchEntity m
            LEFT JOIN FETCH m.tournament
            LEFT JOIN FETCH m.player1
            LEFT JOIN FETCH m.player2
            LEFT JOIN FETCH m.winner
            WHERE m.tournament.id = :tournamentId AND m.round = :round
            """)
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
