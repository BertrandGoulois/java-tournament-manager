package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.port.out.registration.CountRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.ExistsRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.SaveRegistrationPort;
import com.tournament.tournament_manager.repository.RegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de gestion des inscriptions.
 */
@Component
public class RegistrationJpaAdapter implements LoadRegistrationPort, SaveRegistrationPort,
        ExistsRegistrationPort, CountRegistrationPort {

    private final RegistrationRepository registrationRepository;

    public RegistrationJpaAdapter(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    public List<Registration> loadByTournamentId(Long tournamentId) {
        return registrationRepository.findByTournamentId(tournamentId);
    }

    @Override
    public Registration saveRegistration(Registration registration) {
        return registrationRepository.save(registration);
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