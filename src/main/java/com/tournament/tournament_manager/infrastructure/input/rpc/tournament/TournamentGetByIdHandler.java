package com.tournament.tournament_manager.infrastructure.input.rpc.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.in.tournament.GetTournamentUseCase;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code tournament.getById}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du tournoi).
 */
@Component
public class TournamentGetByIdHandler extends AbstractJsonRpcHandler {

    private final GetTournamentUseCase getTournamentUseCase;

    public TournamentGetByIdHandler(GetTournamentUseCase getTournamentUseCase, ObjectMapper objectMapper) {
        super(objectMapper);
        this.getTournamentUseCase = getTournamentUseCase;
    }

    @Override
    public String methodName() {
        return "tournament.getById";
    }

    @Override
    public Object handle(Object params) {
        return getTournamentUseCase.getTournamentById(getLong(params, "id"));
    }
}