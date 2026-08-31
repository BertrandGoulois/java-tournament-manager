package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.EloHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EloHistoryRepository extends JpaRepository<EloHistoryEntity, Long> {

    /**
     * Point 35 de la revue : N+1 caché, corrigé ici. {@code EloHistoryEntity.player} et
     * {@code .match} sont en {@code @ManyToOne} sans {@code fetch} explicite (EAGER par
     * défaut JPA) - {@code MatchMapper.toDomain} reconstruit ensuite un {@code Match}
     * complet à chaque ligne (tournoi + 2 joueurs + vainqueur), ce qui déclenchait, sans
     * ce {@code JOIN FETCH}, une requête SQL séparée par relation et par ligne d'historique
     * (potentiellement N×5+ requêtes pour N entrées). Une seule requête désormais, quel
     * que soit le nombre d'entrées.
     */
    @Query("""
            SELECT eh FROM EloHistoryEntity eh
            LEFT JOIN FETCH eh.match m
            LEFT JOIN FETCH m.tournament
            LEFT JOIN FETCH m.player1
            LEFT JOIN FETCH m.player2
            LEFT JOIN FETCH m.winner
            WHERE eh.player.id = :playerId
            ORDER BY eh.createdAt DESC
            """)
    List<EloHistoryEntity> findByPlayerIdOrderByCreatedAtDesc(@Param("playerId") Long playerId);

    boolean existsByMatchId(Long matchId);
}
