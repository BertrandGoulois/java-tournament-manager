package com.tournament.tournament_manager.application.registration;

import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.port.in.registration.GetRegistrationsUseCase;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.dto.response.registration.RegistrationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation des inscriptions d'un tournoi.
 */
@Service
@Transactional(readOnly = true)
public class GetRegistrationsService implements GetRegistrationsUseCase {

    private final LoadRegistrationPort loadRegistrationPort;

    public GetRegistrationsService(LoadRegistrationPort loadRegistrationPort) {
        this.loadRegistrationPort = loadRegistrationPort;
    }

    @Override
    public Page<RegistrationResponse> getTournamentRegistrations(Long tournamentId, Pageable pageable) {
        return loadRegistrationPort.loadByTournamentId(tournamentId, pageable)
                .map(this::toResponse);
    }

    private RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getPlayer().getId(),
                registration.getTournament().getId(),
                registration.getRegisteredAt()
        );
    }
}