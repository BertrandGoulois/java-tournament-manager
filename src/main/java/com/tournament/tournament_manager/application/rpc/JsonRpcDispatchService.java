package com.tournament.tournament_manager.application.rpc;

import com.tournament.tournament_manager.domain.port.out.rpc.JsonRpcMethodHandler;
import com.tournament.tournament_manager.dto.request.rpc.JsonRpcRequest;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcError;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import com.tournament.tournament_manager.exception.domain.AlreadyExistsException;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
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
 *
 * <p>La validation du champ {@code jsonrpc} (doit valoir {@code "2.0"}) et la gestion des
 * notifications (requête sans {@code id}, aucune réponse à renvoyer) sont la responsabilité
 * de {@code JsonRpcController}, pas de cette classe : ce sont des préoccupations de
 * transport, pas de dispatch métier. Ce service reçoit toujours une requête déjà validée à
 * ce niveau.
 */
@Slf4j
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
     * <p>Politique d'exposition des messages d'erreur — alignée sur {@code GlobalExceptionHandler}
     * côté REST : seules les exceptions métier "curées" ({@link NotFoundException},
     * {@link AlreadyExistsException}, {@link InvalidException}) exposent leur message au client.
     * Toute autre exception (Hibernate, JDBC, etc.) est masquée par un message générique — son
     * détail réel (noms de tables, contraintes, SQL...) n'a rien à faire chez l'appelant — et
     * journalisée côté serveur pour le diagnostic.
     *
     * <p>Les erreurs métier ({@link JsonRpcError#BUSINESS_ERROR}), d'accès
     * ({@link JsonRpcError#ACCESS_DENIED}) et de conflit ({@link JsonRpcError#CONFLICT}) sont
     * distinguées des vraies erreurs internes ({@link JsonRpcError#INTERNAL_ERROR}) : c'est ce
     * qui permet à {@code JsonRpcController} de leur associer des statuts HTTP différents, et
     * donc à toute supervision basée sur les taux de 5xx de rester pertinente sur ce canal.
     *
     * @param request la requête JSON-RPC désérialisée et déjà validée par le contrôleur
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
        } catch (NotFoundException | AlreadyExistsException | InvalidException e) {
            return JsonRpcResponse.failure(
                    new JsonRpcError(JsonRpcError.BUSINESS_ERROR, "Request failed", e.getMessage()),
                    request.id());
        } catch (AccessDeniedException e) {
            // Déclenché par @PreAuthorize sur les handlers réservés ADMIN (voir SecurityConfig) :
            // même politique que côté REST, un accès refusé est un cas attendu, pas une panne.
            log.warn("Accès refusé sur la méthode JSON-RPC '{}'", request.method());
            return JsonRpcResponse.failure(
                    new JsonRpcError(JsonRpcError.ACCESS_DENIED, "Forbidden",
                            "Vous n'avez pas les droits nécessaires pour cette opération"),
                    request.id());
        } catch (ObjectOptimisticLockingFailureException e) {
            // Même traitement que côté REST (GlobalExceptionHandler) : un conflit de
            // modification concurrente est un cas métier attendu, avec un message clair
            // pour l'appelant — pas une exception technique à masquer.
            log.warn("Conflit de modification concurrente sur la méthode JSON-RPC '{}' : {}",
                    request.method(), e.getMessage());
            return JsonRpcResponse.failure(
                    new JsonRpcError(JsonRpcError.CONFLICT, "Conflict",
                            "Cette ressource a été modifiée entre-temps par quelqu'un d'autre. "
                                    + "Recharge les données à jour et réessaie."),
                    request.id());
        } catch (Exception e) {
            log.error("Erreur inattendue lors du traitement de la méthode JSON-RPC '{}' : {}",
                    request.method(), e.getMessage(), e);
            return JsonRpcResponse.failure(
                    new JsonRpcError(JsonRpcError.INTERNAL_ERROR, "Internal error",
                            "Une erreur inattendue s'est produite"),
                    request.id());
        }
    }
}