package com.tournament.tournament_manager.service.rpc;

import com.tournament.tournament_manager.domain.port.out.rpc.JsonRpcMethodHandler;
import com.tournament.tournament_manager.dto.request.rpc.JsonRpcRequest;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcError;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dispatche les requêtes JSON-RPC 2.0 vers le {@link JsonRpcMethodHandler} correspondant.
 *
 * <p>Spring injecte automatiquement tous les handlers déclarés comme {@code @Component},
 * indexés ici par leur nom de méthode (ex. {@code tournament.create}), à la manière du
 * pattern Strategy déjà utilisé pour {@code TournamentStartStrategy}.
 */
@Service
public class JsonRpcDispatchService {

    private final Map<String, JsonRpcMethodHandler> handlers;

    public JsonRpcDispatchService(List<JsonRpcMethodHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(JsonRpcMethodHandler::methodName, h -> h));
    }

    /**
     * Traite une requête JSON-RPC et retourne la réponse correspondante.
     *
     * <p>Ne propage jamais d'exception : toute erreur (méthode inconnue, paramètres
     * invalides, exception métier) est convertie en {@link JsonRpcError} conforme
     * à la spécification, pour que l'appelant reçoive toujours une enveloppe JSON-RPC valide.
     *
     * @param request la requête JSON-RPC désérialisée
     * @return la réponse JSON-RPC (succès ou erreur)
     */
    public JsonRpcResponse dispatch(JsonRpcRequest request) {
        JsonRpcMethodHandler handler = handlers.get(request.method());

        if (handler == null) {
            return JsonRpcResponse.failure(
                    new JsonRpcError(JsonRpcError.METHOD_NOT_FOUND,
                            "Method not found: " + request.method(), null),
                    request.id());
        }

        try {
            Object result = handler.handle(request.params());
            return JsonRpcResponse.success(result, request.id());
        } catch (IllegalArgumentException e) {
            return JsonRpcResponse.failure(
                    new JsonRpcError(JsonRpcError.INVALID_PARAMS, "Invalid params", e.getMessage()),
                    request.id());
        } catch (Exception e) {
            return JsonRpcResponse.failure(
                    new JsonRpcError(JsonRpcError.INTERNAL_ERROR, "Internal error", e.getMessage()),
                    request.id());
        }
    }
}