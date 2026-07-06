package com.tournament.tournament_manager.infrastructure.rpc.player;

import com.tournament.tournament_manager.domain.port.in.player.DeletePlayerUseCase;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code player.delete}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du joueur).
 */
@Component
public class PlayerDeleteHandler extends AbstractJsonRpcHandler {

    private final DeletePlayerUseCase deletePlayerUseCase;

    public PlayerDeleteHandler(DeletePlayerUseCase deletePlayerUseCase) {
        this.deletePlayerUseCase = deletePlayerUseCase;
    }

    @Override
    public String methodName() {
        return "player.delete";
    }

    @Override
    public Object handle(Object params) {
        deletePlayerUseCase.deletePlayer(getLong(params, "id"));
        return null;
    }
}