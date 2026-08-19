package com.tournament.tournament_manager.infrastructure.input.rpc.player;

import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerStatsUseCase;
import com.tournament.tournament_manager.infrastructure.input.mapper.PlayerRestMapper;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code player.getStats}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du joueur).
 */
@Component
public class PlayerGetStatsHandler extends AbstractJsonRpcHandler {

    private final GetPlayerStatsUseCase getPlayerStatsUseCase;
    private final PlayerRestMapper playerRestMapper;

    public PlayerGetStatsHandler(GetPlayerStatsUseCase getPlayerStatsUseCase, ObjectMapper objectMapper,
                                 Validator validator, PlayerRestMapper playerRestMapper) {
        super(objectMapper, validator);
        this.getPlayerStatsUseCase = getPlayerStatsUseCase;
        this.playerRestMapper = playerRestMapper;
    }

    @Override
    public String methodName() {
        return "player.getStats";
    }

    @Override
    public Object handle(Object params) {
        return playerRestMapper.toStatsResponse(getPlayerStatsUseCase.getPlayerStats(getLong(params, "id")));
    }
}
