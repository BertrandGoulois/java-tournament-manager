package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.port.in.tournament.GetTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadAllTournamentsPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation d'un ou plusieurs tournois. Retourne des objets de
 * domaine purs — voir la Javadoc de {@code GetPlayerService}.
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
    public Tournament getTournamentById(Long id) {
        return loadTournamentPort.loadTournament(id);
    }

    @Override
    public PageResult<Tournament> getAllTournaments(PageRequest pageRequest) {
        return loadAllTournamentsPort.loadAllTournaments(pageRequest);
    }
}
