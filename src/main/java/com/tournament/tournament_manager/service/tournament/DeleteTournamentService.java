package com.tournament.tournament_manager.service.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.in.tournament.DeleteTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SoftDeleteTournamentPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cas d'utilisation : suppression (soft delete) d'un tournoi.
 */
@Service
@Transactional
public class DeleteTournamentService implements DeleteTournamentUseCase {

    private final LoadTournamentPort loadTournamentPort;
    private final SoftDeleteTournamentPort softDeleteTournamentPort;

    public DeleteTournamentService(LoadTournamentPort loadTournamentPort,
                                   SoftDeleteTournamentPort softDeleteTournamentPort) {
        this.loadTournamentPort = loadTournamentPort;
        this.softDeleteTournamentPort = softDeleteTournamentPort;
    }

    @Override
    public void deleteTournament(Long id) {
        Tournament tournament = loadTournamentPort.loadTournament(id);
        tournament.setDeleted(true);
        tournament.setDeletedAt(LocalDateTime.now());
        softDeleteTournamentPort.softDeleteTournament(tournament);
    }
}