package com.tournament.tournament_manager.infrastructure.rpc.registration;

import com.tournament.tournament_manager.domain.port.in.registration.GetRegistrationsUseCase;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code registration.getByTournament}.
 *
 * <p>Attend un paramètre {@code tournamentId}.
 */
@Component
public class RegistrationGetByTournamentHandler extends AbstractJsonRpcHandler {

    private final GetRegistrationsUseCase getRegistrationsUseCase;

    public RegistrationGetByTournamentHandler(GetRegistrationsUseCase getRegistrationsUseCase) {
        this.getRegistrationsUseCase = getRegistrationsUseCase;
    }

    @Override
    public String methodName() {
        return "registration.getByTournament";
    }

    @Override
    public Object handle(Object params) {
        return getRegistrationsUseCase.getTournamentRegistrations(getLong(params, "tournamentId"));
    }
}