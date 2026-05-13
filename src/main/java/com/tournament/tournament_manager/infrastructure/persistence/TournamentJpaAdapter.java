package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.out.ExistsTournamentPort;
import com.tournament.tournament_manager.domain.port.out.LoadAllTournamentsPort;
import com.tournament.tournament_manager.domain.port.out.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.SaveTournamentPort;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de chargement et sauvegarde des tournois.
 */
@Component
public class TournamentJpaAdapter implements LoadTournamentPort, SaveTournamentPort,
        ExistsTournamentPort, LoadAllTournamentsPort {

    private final TournamentRepository tournamentRepository;

    public TournamentJpaAdapter(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Override
    public Tournament loadTournament(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));
    }

    @Override
    public Tournament saveTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    @Override
    public boolean existsByName(String name) {
        return tournamentRepository.existsByName(name);
    }

    @Override
    public List<Tournament> loadAllTournaments() {
        return tournamentRepository.findAll();
    }
}