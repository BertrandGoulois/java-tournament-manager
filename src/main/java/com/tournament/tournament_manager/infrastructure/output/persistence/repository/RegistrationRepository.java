package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RegistrationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<RegistrationEntity, Long> {

    boolean existsByPlayerIdAndTournamentId(Long playerId, Long tournamentId);

    @Query("SELECT r FROM RegistrationEntity r " +
            "LEFT JOIN FETCH r.player " +
            "LEFT JOIN FETCH r.tournament " +
            "WHERE r.tournament.id = :tournamentId")
    List<RegistrationEntity> findByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query(value = "SELECT r FROM RegistrationEntity r " +
            "LEFT JOIN FETCH r.player " +
            "LEFT JOIN FETCH r.tournament " +
            "WHERE r.tournament.id = :tournamentId",
            countQuery = "SELECT COUNT(r) FROM RegistrationEntity r WHERE r.tournament.id = :tournamentId")
    Page<RegistrationEntity> findByTournamentId(@Param("tournamentId") Long tournamentId, Pageable pageable);

    long countByTournamentId(Long tournamentId);
}
