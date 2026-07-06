package com.tournament.tournament_manager.infrastructure.rpc.player;

import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code player.getAll}.
 *
 * <p>Paramètres optionnels {@code page} et {@code size} (défauts : 0 et 10).
 */
@Component
public class PlayerGetAllHandler extends AbstractJsonRpcHandler {

    private final GetPlayerUseCase getPlayerUseCase;

    public PlayerGetAllHandler(GetPlayerUseCase getPlayerUseCase) {
        this.getPlayerUseCase = getPlayerUseCase;
    }

    @Override
    public String methodName() {
        return "player.getAll";
    }

    @Override
    public Object handle(Object params) {
        java.util.Map<?, ?> map = params != null ? objectMapper.convertValue(params, java.util.Map.class) : java.util.Map.of();
        int page = map.containsKey("page") ? ((Number) map.get("page")).intValue() : 0;
        int size = map.containsKey("size") ? ((Number) map.get("size")).intValue() : 10;
        return getPlayerUseCase.getAllPlayers(org.springframework.data.domain.PageRequest.of(page, size));
    }
}