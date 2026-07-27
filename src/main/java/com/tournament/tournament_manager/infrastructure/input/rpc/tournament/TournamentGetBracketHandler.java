package com.tournament.tournament_manager.infrastructure.input.rpc.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.tournament.GetBracketUseCase;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code tournament.getBracket}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du tournoi).
 */
@Component
public class TournamentGetBracketHandler extends AbstractJsonRpcHandler {

    private final GetBracketUseCase getBracketUseCase;

    public TournamentGetBracketHandler(GetBracketUseCase getBracketUseCase, ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
        this.getBracketUseCase = getBracketUseCase;
    }

    @Override
    public String methodName() {
        return "tournament.getBracket";
    }

    @Override
    public Object handle(Object params) {
        return getBracketUseCase.getBracket(getLong(params, "id"));
    }
}