package com.tournament.tournament_manager.infrastructure.input.rpc.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.in.tournament.CreateTournamentUseCase;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code tournament.create}.
 *
 * <p>Réutilise directement {@link CreateTournamentUseCase} : aucune logique métier
 * dupliquée entre l'API REST ({@code POST /api/tournaments}) et JSON-RPC, seule
 * la désérialisation des paramètres diffère.
 */
@Component
public class TournamentCreateHandler extends AbstractJsonRpcHandler {

    private final CreateTournamentUseCase createTournamentUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TournamentCreateHandler(CreateTournamentUseCase createTournamentUseCase, ObjectMapper objectMapper) {
        super(objectMapper);
        this.createTournamentUseCase = createTournamentUseCase;
    }

    @Override
    public String methodName() {
        return "tournament.create";
    }

    @Override
    public Object handle(Object params) {
        CreateTournamentRequest request = convertParams(params, CreateTournamentRequest.class);
        return createTournamentUseCase.createTournament(request);
    }

}