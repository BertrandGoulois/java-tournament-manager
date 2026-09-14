package com.tournament.tournament_manager.infrastructure.input.rpc.tournament;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import com.tournament.tournament_manager.domain.port.in.tournament.GetBracketUseCase;
import com.tournament.tournament_manager.infrastructure.input.mapper.TournamentRestMapper;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;

import tools.jackson.databind.ObjectMapper;

/**
 * Handler JSON-RPC de la méthode {@code tournament.getBracket}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du tournoi).
 */
@Component
public class TournamentGetBracketHandler extends AbstractJsonRpcHandler {

    private final GetBracketUseCase getBracketUseCase;
    private final TournamentRestMapper tournamentRestMapper;

    public TournamentGetBracketHandler(GetBracketUseCase getBracketUseCase, ObjectMapper objectMapper,
                                       Validator validator, TournamentRestMapper tournamentRestMapper) {
        super(objectMapper, validator);
        this.getBracketUseCase = getBracketUseCase;
        this.tournamentRestMapper = tournamentRestMapper;
    }

    @Override
    public String methodName() {
        return "tournament.getBracket";
    }

    @Override
    public Object handle(Object params) {
        return tournamentRestMapper.toResponse(getBracketUseCase.getBracket(getLong(params, "id")));
    }
}
