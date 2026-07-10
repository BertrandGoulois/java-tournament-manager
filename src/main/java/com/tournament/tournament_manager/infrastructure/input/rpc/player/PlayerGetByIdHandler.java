package com.tournament.tournament_manager.infrastructure.input.rpc.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code player.getById}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du joueur).
 */
@Component
public class PlayerGetByIdHandler extends AbstractJsonRpcHandler {

    private final GetPlayerUseCase getPlayerUseCase;

    public PlayerGetByIdHandler(GetPlayerUseCase getPlayerUseCase, ObjectMapper objectMapper) {
        super(objectMapper);
        this.getPlayerUseCase = getPlayerUseCase;
    }

    @Override
    public String methodName() {
        return "player.getById";
    }

    @Override
    public Object handle(Object params) {
        return getPlayerUseCase.getPlayerById(getLong(params, "id"));
    }
}