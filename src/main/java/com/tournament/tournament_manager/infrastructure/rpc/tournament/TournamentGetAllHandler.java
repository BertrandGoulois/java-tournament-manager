package com.tournament.tournament_manager.infrastructure.rpc.tournament;

import com.tournament.tournament_manager.domain.port.in.tournament.GetTournamentUseCase;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code tournament.getAll}.
 *
 * <p>Paramètres optionnels {@code page} et {@code size} (défauts : 0 et 10).
 */
@Component
public class TournamentGetAllHandler extends AbstractJsonRpcHandler {

    private final GetTournamentUseCase getTournamentUseCase;

    public TournamentGetAllHandler(GetTournamentUseCase getTournamentUseCase) {
        this.getTournamentUseCase = getTournamentUseCase;
    }

    @Override
    public String methodName() {
        return "tournament.getAll";
    }

    @Override
    public Object handle(Object params) {
        java.util.Map<?, ?> map = params != null ? objectMapper.convertValue(params, java.util.Map.class) : java.util.Map.of();
        int page = map.containsKey("page") ? ((Number) map.get("page")).intValue() : 0;
        int size = map.containsKey("size") ? ((Number) map.get("size")).intValue() : 10;
        return getTournamentUseCase.getAllTournaments(org.springframework.data.domain.PageRequest.of(page, size));
    }
}