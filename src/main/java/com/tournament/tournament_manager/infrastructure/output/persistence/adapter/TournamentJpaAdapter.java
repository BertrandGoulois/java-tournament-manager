package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgeTournamentsPort;
import com.tournament.tournament_manager.domain.port.out.tournament.*;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.TournamentMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Adapter JPA implémentant les ports de chargement et sauvegarde des tournois.
 * Voir {@code PlayerJpaAdapter} pour le principe du mapper et le traitement du
 * verrouillage optimiste.
 */
@Component
public class TournamentJpaAdapter implements LoadTournamentPort, SaveTournamentPort,
        ExistsTournamentPort, LoadAllTournamentsPort, SoftDeleteTournamentPort, PurgeTournamentsPort {

    private final TournamentRepository tournamentRepository;
    private final TournamentMapper tournamentMapper;

    public TournamentJpaAdapter(TournamentRepository tournamentRepository, TournamentMapper tournamentMapper) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentMapper = tournamentMapper;
    }

    @Override
    public Tournament loadTournament(Long id) {
        TournamentEntity entity = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));
        return tournamentMapper.toDomain(entity);
    }

    @Override
    public Tournament saveTournament(Tournament tournament) {
        TournamentEntity entity;
        if (tournament.getId() != null) {
            entity = tournamentRepository.findById(tournament.getId())
                    .orElseThrow(() -> new TournamentNotFoundException(tournament.getId()));
            tournamentMapper.updateEntity(entity, tournament);
        } else {
            entity = tournamentMapper.toNewEntity(tournament);
        }
        TournamentEntity saved = tournamentRepository.save(entity);
        return tournamentMapper.toDomain(saved);
    }

    @Override
    public boolean existsByName(String name) {
        return tournamentRepository.existsByName(name);
    }

    @Override
    public PageResult<Tournament> loadAllTournaments(PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        var page = tournamentRepository.findAll(pageable).map(tournamentMapper::toDomain);
        return PageResult.of(page.getContent(), pageRequest.page(), pageRequest.size(), page.getTotalElements());
    }

    @Override
    public void softDeleteTournament(Tournament tournament) {
        saveTournament(tournament);
    }

    @Override
    public int purgeDeletedBefore(Instant retentionLimit) {
        return tournamentRepository.purgeDeletedBefore(retentionLimit);
    }
}
