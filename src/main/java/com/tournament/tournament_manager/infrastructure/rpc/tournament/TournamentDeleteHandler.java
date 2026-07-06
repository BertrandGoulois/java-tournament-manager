package com.tournament.tournament_manager.infrastructure.rpc.tournament;

import com.tournament.tournament_manager.domain.port.in.tournament.DeleteTournamentUseCase;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code tournament.delete}.
 *
 * <p>Attend un paramètre {@code id} (identifiant du tournoi).
 */
@Component
public class TournamentDeleteHandler extends AbstractJsonRpcHandler {

    private final DeleteTournamentUseCase deleteTournamentUseCase;

    public TournamentDeleteHandler(DeleteTournamentUseCase deleteTournamentUseCase) {
        this.deleteTournamentUseCase = deleteTournamentUseCase;
    }

    @Override
    public String methodName() {
        return "tournament.delete";
    }

    @Override
    public Object handle(Object params) {
        deleteTournamentUseCase.deleteTournament(getLong(params, "id"));
        return null;
    }
}