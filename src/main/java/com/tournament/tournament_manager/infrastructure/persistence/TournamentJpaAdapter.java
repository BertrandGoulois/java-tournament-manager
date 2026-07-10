package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.out.tournament.*;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter JPA implémentant les ports de chargement et sauvegarde des tournois.
 */
@Component
public class TournamentJpaAdapter implements LoadTournamentPort, SaveTournamentPort,
        ExistsTournamentPort, LoadAllTournamentsPort, SoftDeleteTournamentPort {

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
    public Page<Tournament> loadAllTournaments(Pageable pageable) {
        return tournamentRepository.findAll(pageable);
    }

    @Override
    public void softDeleteTournament(Tournament tournament) {
        tournamentRepository.save(tournament);
    }
}