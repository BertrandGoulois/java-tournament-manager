package com.tournament.tournament_manager.infrastructure.input.rpc.tournament;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.tournament.GetStandingsUseCase;
import com.tournament.tournament_manager.infrastructure.input.mapper.TournamentRestMapper;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code tournament.getStandings}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du tournoi).
 */
@Component
public class TournamentGetStandingsHandler extends AbstractJsonRpcHandler {

    private final GetStandingsUseCase getStandingsUseCase;
    private final TournamentRestMapper tournamentRestMapper;

    public TournamentGetStandingsHandler(GetStandingsUseCase getStandingsUseCase, ObjectMapper objectMapper,
                                         Validator validator, TournamentRestMapper tournamentRestMapper) {
        super(objectMapper, validator);
        this.getStandingsUseCase = getStandingsUseCase;
        this.tournamentRestMapper = tournamentRestMapper;
    }

    @Override
    public String methodName() {
        return "tournament.getStandings";
    }

    @Override
    public Object handle(Object params) {
        return tournamentRestMapper.toResponse(getStandingsUseCase.getStandings(getLong(params, "id")));
    }
}
