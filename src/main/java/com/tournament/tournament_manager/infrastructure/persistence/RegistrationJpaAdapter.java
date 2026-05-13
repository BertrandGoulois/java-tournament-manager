package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.port.out.LoadRegistrationPort;
import com.tournament.tournament_manager.repository.RegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant le chargement des inscriptions d'un tournoi.
 */
@Component
public class RegistrationJpaAdapter implements LoadRegistrationPort {

    private final RegistrationRepository registrationRepository;

    public RegistrationJpaAdapter(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    public List<Registration> loadByTournamentId(Long tournamentId) {
        return registrationRepository.findByTournamentId(tournamentId);
    }
}