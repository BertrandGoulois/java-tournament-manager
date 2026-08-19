package com.tournament.tournament_manager.infrastructure.input.mapper;

import com.tournament.tournament_manager.domain.model.RegisterPlayerCommand;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.registration.RegistrationResponse;
import org.springframework.stereotype.Component;

/**
 * Convertit entre le domaine pur ({@link Registration}) et les DTO REST. Voir la Javadoc
 * de {@code PlayerRestMapper}.
 */
@Component
public class RegistrationRestMapper {

    public RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getPlayer().getId(),
                registration.getTournament().getId(),
                registration.getRegisteredAt()
        );
    }

    public RegisterPlayerCommand toCommand(CreateRegistrationRequest request) {
        return new RegisterPlayerCommand(request.playerId(), request.tournamentId());
    }
}
