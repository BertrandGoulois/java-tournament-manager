package com.tournament.tournament_manager.infrastructure.input.rpc.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code player.create}.
 */
@Component
public class PlayerCreateHandler extends AbstractJsonRpcHandler {

    private final CreatePlayerUseCase createPlayerUseCase;

    public PlayerCreateHandler(CreatePlayerUseCase createPlayerUseCase, ObjectMapper objectMapper,
                               Validator validator) {
        super(objectMapper, validator);
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