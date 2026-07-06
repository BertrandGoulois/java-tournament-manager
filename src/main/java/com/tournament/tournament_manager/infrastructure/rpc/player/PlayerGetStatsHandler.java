package com.tournament.tournament_manager.infrastructure.rpc.player;

import com.tournament.tournament_manager.domain.port.in.player.GetPlayerStatsUseCase;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code player.getStats}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du joueur).
 */
@Component
public class PlayerGetStatsHandler extends AbstractJsonRpcHandler {

    private final GetPlayerStatsUseCase getPlayerStatsUseCase;

    public PlayerGetStatsHandler(GetPlayerStatsUseCase getPlayerStatsUseCase) {
        this.getPlayerStatsUseCase = getPlayerStatsUseCase;
    }

    @Override
    public String methodName() {
        return "player.getStats";
    }

    @Override
    public Object handle(Object params) {
        return getPlayerStatsUseCase.getPlayerStats(getLong(params, "id"));
    }
}