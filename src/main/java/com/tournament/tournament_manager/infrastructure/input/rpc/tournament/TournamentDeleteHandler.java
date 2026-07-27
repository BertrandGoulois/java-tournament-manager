package com.tournament.tournament_manager.infrastructure.input.rpc.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.tournament.DeleteTournamentUseCase;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code tournament.delete}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du tournoi).
 */
@Component
public class TournamentDeleteHandler extends AbstractJsonRpcHandler {

    private final DeleteTournamentUseCase deleteTournamentUseCase;

    public TournamentDeleteHandler(DeleteTournamentUseCase deleteTournamentUseCase, ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
        this.deleteTournamentUseCase = deleteTournamentUseCase;
    }

    @Override
    public String methodName() {
        return "tournament.delete";
    }

    @Override
    public Object handle(Object params) {
        deleteTournamentUseCase.deleteTournament(getLong(params, "id"));
        return null;
    }
}