package com.tournament.tournament_manager.application.registration;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.port.in.registration.GetRegistrationsUseCase;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation des inscriptions d'un tournoi. Retourne des objets de
 * domaine purs — voir la Javadoc de {@code GetPlayerService}.
 */
@Service
@Transactional(readOnly = true)
public class GetRegistrationsService implements GetRegistrationsUseCase {

    private final LoadRegistrationPort loadRegistrationPort;

    public GetRegistrationsService(LoadRegistrationPort loadRegistrationPort) {
        this.loadRegistrationPort = loadRegistrationPort;
    }

    @Override
    public PageResult<Registration> getTournamentRegistrations(Long tournamentId, PageRequest pageRequest) {
        return loadRegistrationPort.loadByTournamentId(tournamentId, pageRequest);
    }
}
