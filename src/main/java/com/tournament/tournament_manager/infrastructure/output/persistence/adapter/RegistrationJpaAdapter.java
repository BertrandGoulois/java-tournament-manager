package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.port.out.registration.CountRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.ExistsRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.SaveRegistrationPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RegistrationEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.RegistrationMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RegistrationRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de gestion des inscriptions.
 * Voir {@code MatchJpaAdapter} pour le principe de résolution de références.
 */
@Component
public class RegistrationJpaAdapter implements LoadRegistrationPort, SaveRegistrationPort,
        ExistsRegistrationPort, CountRegistrationPort {

    private final RegistrationRepository registrationRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final RegistrationMapper registrationMapper;

    public RegistrationJpaAdapter(RegistrationRepository registrationRepository,
                                  PlayerRepository playerRepository,
                                  TournamentRepository tournamentRepository,
                                  RegistrationMapper registrationMapper) {
        this.registrationRepository = registrationRepository;
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
        this.registrationMapper = registrationMapper;
    }

    @Override
    public List<Registration> loadByTournamentId(Long tournamentId) {
        return registrationRepository.findByTournamentId(tournamentId).stream()
                .map(registrationMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<Registration> loadByTournamentId(Long tournamentId, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        var page = registrationRepository.findByTournamentId(tournamentId, pageable)
                .map(registrationMapper::toDomain);
        return PageResult.of(page.getContent(), pageRequest.page(), pageRequest.size(), page.getTotalElements());
    }

    @Override
    public Registration saveRegistration(Registration registration) {
        TournamentEntity tournamentRef = tournamentRepository.getReferenceById(registration.getTournament().getId());
        PlayerEntity playerRef = playerRepository.getReferenceById(registration.getPlayer().getId());
        RegistrationEntity entity = registrationMapper.toNewEntity(tournamentRef, playerRef);
        RegistrationEntity saved = registrationRepository.save(entity);
        return registrationMapper.toDomain(saved);
    }

    @Override
    public boolean existsByPlayerIdAndTournamentId(Long playerId, Long tournamentId) {
        return registrationRepository.existsByPlayerIdAndTournamentId(playerId, tournamentId);
    }

    @Override
    public long countByTournamentId(Long tournamentId) {
        return registrationRepository.countByTournamentId(tournamentId);
    }
}
