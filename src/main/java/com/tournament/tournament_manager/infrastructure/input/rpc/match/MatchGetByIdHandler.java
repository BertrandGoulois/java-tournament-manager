package com.tournament.tournament_manager.infrastructure.input.rpc.match;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchUseCase;
import com.tournament.tournament_manager.infrastructure.input.mapper.MatchRestMapper;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code match.getById}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du match).
 */
@Component
public class MatchGetByIdHandler extends AbstractJsonRpcHandler {

    private final GetMatchUseCase getMatchUseCase;
    private final MatchRestMapper matchRestMapper;

    public MatchGetByIdHandler(GetMatchUseCase getMatchUseCase, ObjectMapper objectMapper, Validator validator,
                               MatchRestMapper matchRestMapper) {
        super(objectMapper, validator);
        this.getMatchUseCase = getMatchUseCase;
        this.matchRestMapper = matchRestMapper;
    }

    @Override
    public String methodName() {
        return "match.getById";
    }

    @Override
    public Object handle(Object params) {
        return matchRestMapper.toResponse(getMatchUseCase.getMatchById(getLong(params, "id")));
    }
}
