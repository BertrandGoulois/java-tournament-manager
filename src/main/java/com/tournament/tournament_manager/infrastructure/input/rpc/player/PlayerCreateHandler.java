package com.tournament.tournament_manager.infrastructure.input.rpc.player;

import tools.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.infrastructure.input.mapper.PlayerRestMapper;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code player.create}.
 */
@Component
public class PlayerCreateHandler extends AbstractJsonRpcHandler {

    private final CreatePlayerUseCase createPlayerUseCase;
    private final PlayerRestMapper playerRestMapper;

    public PlayerCreateHandler(CreatePlayerUseCase createPlayerUseCase, ObjectMapper objectMapper,
                               Validator validator, PlayerRestMapper playerRestMapper) {
        super(objectMapper, validator);
        this.createPlayerUseCase = createPlayerUseCase;
        this.playerRestMapper = playerRestMapper;
    }

    @Override
    public String methodName() {
        return "player.create";
    }

    @Override
    public Object handle(Object params) {
        CreatePlayerRequest request = convertParams(params, CreatePlayerRequest.class);
        var player = createPlayerUseCase.createPlayer(playerRestMapper.toCommand(request));
        return playerRestMapper.toResponse(player);
    }
}
