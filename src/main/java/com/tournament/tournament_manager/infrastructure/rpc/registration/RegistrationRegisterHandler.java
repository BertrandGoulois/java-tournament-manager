package com.tournament.tournament_manager.infrastructure.rpc.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.in.registration.RegisterPlayerUseCase;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.infrastructure.rpc.AbstractJsonRpcHandler;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code registration.register}.
 */
@Component
public class RegistrationRegisterHandler extends AbstractJsonRpcHandler {

    private final RegisterPlayerUseCase registerPlayerUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegistrationRegisterHandler(RegisterPlayerUseCase registerPlayerUseCase) {
        this.registerPlayerUseCase = registerPlayerUseCase;
    }

    @Override
    public String methodName() {
        return "registration.register";
    }

    @Override
    public Object handle(Object params) {
        CreateRegistrationRequest request = convertParams(params, CreateRegistrationRequest.class);
        return registerPlayerUseCase.registerPlayer(request);
    }
}