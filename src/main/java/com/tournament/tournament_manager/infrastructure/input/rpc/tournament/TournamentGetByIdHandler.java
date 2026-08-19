package com.tournament.tournament_manager.infrastructure.input.rpc.tournament;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.tournament.GetTournamentUseCase;
import com.tournament.tournament_manager.infrastructure.input.mapper.TournamentRestMapper;
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
    private final TournamentRestMapper tournamentRestMapper;

    public TournamentGetByIdHandler(GetTournamentUseCase getTournamentUseCase, ObjectMapper objectMapper,
                                    Validator validator, TournamentRestMapper tournamentRestMapper) {
        super(objectMapper, validator);
        this.getTournamentUseCase = getTournamentUseCase;
        this.tournamentRestMapper = tournamentRestMapper;
    }

    @Override
    public String methodName() {
        return "tournament.getById";
    }

    @Override
    public Object handle(Object params) {
        return tournamentRestMapper.toResponse(getTournamentUseCase.getTournamentById(getLong(params, "id")));
    }
}
