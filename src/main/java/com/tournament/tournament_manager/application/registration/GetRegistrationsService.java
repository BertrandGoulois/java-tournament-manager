package com.tournament.tournament_manager.application.registration;

import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.port.in.registration.GetRegistrationsUseCase;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.dto.response.registration.RegistrationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<RegistrationResponse> getTournamentRegistrations(Long tournamentId) {
        return loadRegistrationPort.loadByTournamentId(tournamentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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