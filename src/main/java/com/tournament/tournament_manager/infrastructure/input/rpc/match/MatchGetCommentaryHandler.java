package com.tournament.tournament_manager.infrastructure.input.rpc.match;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchCommentaryUseCase;
import com.tournament.tournament_manager.infrastructure.input.mapper.MatchRestMapper;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code match.getCommentary}.
 *
 * <p>Attend un paramètre {@code matchId}.
 */
@Component
public class MatchGetCommentaryHandler extends AbstractJsonRpcHandler {

    private final GetMatchCommentaryUseCase getMatchCommentaryUseCase;
    private final MatchRestMapper matchRestMapper;

    public MatchGetCommentaryHandler(GetMatchCommentaryUseCase getMatchCommentaryUseCase, ObjectMapper objectMapper,
                                     Validator validator, MatchRestMapper matchRestMapper) {
        super(objectMapper, validator);
        this.getMatchCommentaryUseCase = getMatchCommentaryUseCase;
        this.matchRestMapper = matchRestMapper;
    }

    @Override
    public String methodName() {
        return "match.getCommentary";
    }

    @Override
    public Object handle(Object params) {
        return matchRestMapper.toResponse(getMatchCommentaryUseCase.getMatchCommentary(getLong(params, "matchId")));
    }
}
