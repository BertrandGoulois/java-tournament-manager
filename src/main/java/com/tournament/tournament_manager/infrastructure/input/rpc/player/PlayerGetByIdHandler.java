package com.tournament.tournament_manager.infrastructure.input.rpc.player;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.infrastructure.input.mapper.PlayerRestMapper;
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
    private final PlayerRestMapper playerRestMapper;

    public PlayerGetByIdHandler(GetPlayerUseCase getPlayerUseCase, ObjectMapper objectMapper, Validator validator,
                                PlayerRestMapper playerRestMapper) {
        super(objectMapper, validator);
        this.getPlayerUseCase = getPlayerUseCase;
        this.playerRestMapper = playerRestMapper;
    }

    @Override
    public String methodName() {
        return "player.getById";
    }

    @Override
    public Object handle(Object params) {
        return playerRestMapper.toResponse(getPlayerUseCase.getPlayerById(getLong(params, "id")));
    }
}
