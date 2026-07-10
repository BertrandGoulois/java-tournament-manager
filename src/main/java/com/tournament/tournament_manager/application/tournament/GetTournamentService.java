package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.in.tournament.GetTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadAllTournamentsPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation d'un ou plusieurs tournois.
 */
@Service
@Transactional(readOnly = true)
public class GetTournamentService implements GetTournamentUseCase {

    private final LoadTournamentPort loadTournamentPort;
    private final LoadAllTournamentsPort loadAllTournamentsPort;

    public GetTournamentService(LoadTournamentPort loadTournamentPort,
                                LoadAllTournamentsPort loadAllTournamentsPort) {
        this.loadTournamentPort = loadTournamentPort;
        this.loadAllTournamentsPort = loadAllTournamentsPort;
    }

    @Override
    public TournamentResponse getTournamentById(Long id) {
        return toResponse(loadTournamentPort.loadTournament(id));
    }

    @Override
    public Page<TournamentResponse> getAllTournaments(Pageable pageable) {
        return loadAllTournamentsPort.loadAllTournaments(pageable)
                .map(this::toResponse);
    }

    private TournamentResponse toResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getStatus(),
                tournament.getFormat(),
                tournament.getMaxPlayers(),
                tournament.getNumberOfGroups(),
                tournament.getQualifiersPerGroup(),
                tournament.getCreatedAt()
        );
    }
}