package com.tournament.tournament_manager.infrastructure.input.rpc.registration;

import tools.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.in.registration.RegisterPlayerUseCase;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.infrastructure.input.mapper.RegistrationRestMapper;
import com.tournament.tournament_manager.infrastructure.input.rpc.AbstractJsonRpcHandler;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Handler JSON-RPC de la méthode {@code registration.register}.
 */
@Component
public class RegistrationRegisterHandler extends AbstractJsonRpcHandler {

    private final RegisterPlayerUseCase registerPlayerUseCase;
    private final RegistrationRestMapper registrationRestMapper;

    public RegistrationRegisterHandler(RegisterPlayerUseCase registerPlayerUseCase, ObjectMapper objectMapper,
                                       Validator validator, RegistrationRestMapper registrationRestMapper) {
        super(objectMapper, validator);
        this.registerPlayerUseCase = registerPlayerUseCase;
        this.registrationRestMapper = registrationRestMapper;
    }

    @Override
    public String methodName() {
        return "registration.register";
    }

    @Override
    public Object handle(Object params) {
        CreateRegistrationRequest request = convertParams(params, CreateRegistrationRequest.class);
        var registration = registerPlayerUseCase.registerPlayer(registrationRestMapper.toCommand(request));
        return registrationRestMapper.toResponse(registration);
    }
}
