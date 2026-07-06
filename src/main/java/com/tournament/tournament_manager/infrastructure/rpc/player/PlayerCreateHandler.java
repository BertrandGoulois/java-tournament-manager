package com.tournament.tournament_manager.infrastructure.rpc.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code player.create}.
 */
@Component
public class PlayerCreateHandler extends AbstractJsonRpcHandler {

    private final CreatePlayerUseCase createPlayerUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlayerCreateHandler(CreatePlayerUseCase createPlayerUseCase) {
        this.createPlayerUseCase = createPlayerUseCase;
    }

    @Override
    public String methodName() {
        return "player.create";
    }

    @Override
    public Object handle(Object params) {
        CreatePlayerRequest request = convertParams(params, CreatePlayerRequest.class);
        return createPlayerUseCase.createPlayer(request);
    }
}