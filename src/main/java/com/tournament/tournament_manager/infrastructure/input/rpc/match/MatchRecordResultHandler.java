package com.tournament.tournament_manager.infrastructure.input.rpc.match;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.model.RecordMatchResultCommand;
import com.tournament.tournament_manager.domain.port.in.match.RecordMatchResultUseCase;
import com.tournament.tournament_manager.infrastructure.input.mapper.MatchRestMapper;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code match.recordResult}.
 *
 * <p>Attend les paramètres {@code matchId} et {@code winnerId}.
 */
@Component
public class MatchRecordResultHandler extends AbstractJsonRpcHandler {

    private final RecordMatchResultUseCase recordMatchResultUseCase;
    private final MatchRestMapper matchRestMapper;

    public MatchRecordResultHandler(RecordMatchResultUseCase recordMatchResultUseCase, ObjectMapper objectMapper,
                                    Validator validator, MatchRestMapper matchRestMapper) {
        super(objectMapper, validator);
        this.recordMatchResultUseCase = recordMatchResultUseCase;
        this.matchRestMapper = matchRestMapper;
    }

    @Override
    public String methodName() {
        return "match.recordResult";
    }

    @Override
    public Object handle(Object params) {
        Long matchId = getLong(params, "matchId");
        Long winnerId = getLong(params, "winnerId");
        var match = recordMatchResultUseCase.recordMatchResult(matchId, new RecordMatchResultCommand(winnerId));
        return matchRestMapper.toResponse(match);
    }
}
