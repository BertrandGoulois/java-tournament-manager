package com.tournament.tournament_manager.infrastructure.input.rpc.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.match.RecordMatchResultUseCase;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
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

    public MatchRecordResultHandler(RecordMatchResultUseCase recordMatchResultUseCase, ObjectMapper objectMapper, Validator validator) {
        super(objectMapper, validator);
        this.recordMatchResultUseCase = recordMatchResultUseCase;
    }

    @Override
    public String methodName() {
        return "match.recordResult";
    }

    @Override
    public Object handle(Object params) {
        Long matchId = getLong(params, "matchId");
        Long winnerId = getLong(params, "winnerId");
        return recordMatchResultUseCase.recordMatchResult(matchId, new RecordMatchResultRequest(winnerId));
    }
}