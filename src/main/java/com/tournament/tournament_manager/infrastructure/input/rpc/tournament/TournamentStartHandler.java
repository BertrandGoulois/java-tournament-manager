package com.tournament.tournament_manager.infrastructure.input.rpc.tournament;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.tournament.StartTournamentUseCase;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code tournament.start}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du tournoi).
 */
@Component
public class TournamentStartHandler extends AbstractJsonRpcHandler {

    private final StartTournamentUseCase startTournamentUseCase;

    public TournamentStartHandler(StartTournamentUseCase startTournamentUseCase, ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
        this.startTournamentUseCase = startTournamentUseCase;
    }

    @Override
    public String methodName() {
        return "tournament.start";
    }

    @Override
    public Object handle(Object params) {
        startTournamentUseCase.startTournament(getLong(params, "id"));
        return null;
    }
}