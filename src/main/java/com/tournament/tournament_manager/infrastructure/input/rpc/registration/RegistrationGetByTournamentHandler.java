package com.tournament.tournament_manager.infrastructure.input.rpc.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.registration.GetRegistrationsUseCase;
import com.tournament.tournament_manager.dto.request.rpc.RegistrationGetByTournamentParams;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code registration.getByTournament}.
 *
 * <p>Attend un paramètre {@code tournamentId}.
 */
@Component
public class RegistrationGetByTournamentHandler extends AbstractJsonRpcHandler {

    private final GetRegistrationsUseCase getRegistrationsUseCase;

    public RegistrationGetByTournamentHandler(GetRegistrationsUseCase getRegistrationsUseCase, ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
        this.getRegistrationsUseCase = getRegistrationsUseCase;
    }

    @Override
    public String methodName() {
        return "registration.getByTournament";
    }

    @Override
    public Object handle(Object params) {
        RegistrationGetByTournamentParams request =
                objectMapper.convertValue(params, RegistrationGetByTournamentParams.class);

        Pageable pageable = PageRequest.of(request.page(), request.size());

        return getRegistrationsUseCase.getTournamentRegistrations(
                request.tournamentId(),
                pageable
        );
    }
}