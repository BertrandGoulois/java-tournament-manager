package com.tournament.tournament_manager.infrastructure.rpc.match;

import com.tournament.tournament_manager.domain.port.in.match.GetMatchCommentaryUseCase;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code match.getCommentary}.
 *
 * <p>Attend un paramètre {@code matchId}.
 */
@Component
public class MatchGetCommentaryHandler extends AbstractJsonRpcHandler {

    private final GetMatchCommentaryUseCase getMatchCommentaryUseCase;

    public MatchGetCommentaryHandler(GetMatchCommentaryUseCase getMatchCommentaryUseCase) {
        this.getMatchCommentaryUseCase = getMatchCommentaryUseCase;
    }

    @Override
    public String methodName() {
        return "match.getCommentary";
    }

    @Override
    public Object handle(Object params) {
        return getMatchCommentaryUseCase.getMatchCommentary(getLong(params, "matchId"));
    }
}