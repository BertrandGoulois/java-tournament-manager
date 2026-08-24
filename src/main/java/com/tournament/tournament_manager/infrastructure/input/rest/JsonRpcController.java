package com.tournament.tournament_manager.infrastructure.input.rest;

import com.tournament.tournament_manager.application.rpc.JsonRpcDispatchService;
import com.tournament.tournament_manager.dto.request.rpc.JsonRpcRequest;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcError;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Point d'entrée unique pour les requêtes JSON-RPC 2.0.
 *
 * <p>Coexiste avec l'API REST existante. L'authentification reste exclusivement
 * gérée par {@code /api/auth/**}. Chaque méthode porte désormais sa propre exigence
 * d'autorisation (voir {@code SecurityConfig}), plus de règle ADMIN en bloc sur cet
 * endpoint.
 *
 * <p>Trois points de conformité à la spec JSON-RPC 2.0 qui n'étaient pas respectés
 * auparavant, maintenant couverts ici :
 * <ul>
 *   <li><b>Requêtes batch</b> : le corps peut être un objet unique ou un tableau de
 *       requêtes ; chaque élément du tableau est traité indépendamment, les réponses
 *       sont retournées dans un tableau (jamais dans le même ordre garanti par la spec,
 *       mais dans l'ordre de traitement ici).</li>
 *   <li><b>Notifications</b> : une requête sans {@code id} n'attend aucune réponse — elle
 *       est exécutée, mais rien n'est renvoyé au client (204, ou silencieusement omise
 *       d'un batch), même en cas d'erreur.</li>
 *   <li><b>Validation du champ {@code jsonrpc}</b> : une requête dont ce champ ne vaut pas
 *       exactement {@code "2.0"} est rejetée en {@code INVALID_REQUEST} (-32600).</li>
 * </ul>
 *
 * <p>Le statut HTTP n'est plus toujours {@code 200} : il reflète désormais la nature de
 * l'erreur (voir {@link #httpStatusFor}), pour qu'une supervision basée sur les taux de
 * 5xx reste pertinente sur ce canal — c'était un angle mort avant.
 */
@Slf4j
@RestController
@RequestMapping("/api/rpc")
@Tag(name = "JSON-RPC", description = "API JSON-RPC 2.0 - endpoint unique exposant toutes les opérations métier")
public class JsonRpcController {

    private final JsonRpcDispatchService dispatchService;
    private final ObjectMapper objectMapper;

    public JsonRpcController(JsonRpcDispatchService dispatchService, ObjectMapper objectMapper) {
        this.dispatchService = dispatchService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Exécuter une ou plusieurs méthodes JSON-RPC",
            description = """
                    Endpoint unique JSON-RPC 2.0. Le champ `method` détermine l'opération à exécuter.
                    Accepte soit un objet requête unique, soit un tableau de requêtes (batch).
                    
                    Méthodes disponibles :
                    - `tournament.create`, `tournament.start`, `tournament.getById`, `tournament.getAll`, `tournament.delete`, `tournament.getBracket`, `tournament.getStandings`
                    - `player.create`, `player.getById`, `player.getAll`, `player.getStats`, `player.delete`
                    - `registration.register`, `registration.getByTournament`
                    - `match.getById`, `match.recordResult`, `match.getCommentary`
                    
                    Le statut HTTP reflète la nature de l'erreur (200 succès ou erreur protocolaire,
                    400 requête/paramètres invalides, 403 accès refusé, 409 conflit, 500 erreur interne).
                    Une requête sans `id` (notification) ne reçoit aucune réponse (204).
                    Codes d'erreur : `-32600` requête invalide, `-32601` méthode inconnue,
                    `-32602` paramètres invalides, `-32603` erreur interne, `-32000` erreur métier,
                    `-32001` accès refusé, `-32002` conflit.
                    """)
    @ApiResponse(responseCode = "200", description = "Réponse JSON-RPC (succès ou erreur protocolaire)",
            content = @Content(schema = @Schema(implementation = JsonRpcResponse.class)))
    @PostMapping
    public ResponseEntity<?> handle(@RequestBody JsonNode body) {
        if (body.isArray()) {
            return handleBatch(body);
        }
        if (body.isObject()) {
            return handleSingle(body);
        }
        return invalidRequestResponse(null, "Request must be a JSON object or an array of objects");
    }

    private ResponseEntity<?> handleSingle(JsonNode node) {
        JsonRpcRequest request = parseOrNull(node);
        if (request == null) {
            return invalidRequestResponse(extractId(node), "Malformed request or missing/invalid 'jsonrpc' field");
        }

        JsonRpcResponse response = dispatchService.dispatch(request);

        if (request.id() == null) {
            // Notification : exécutée (les effets de bord ont eu lieu dans dispatch()),
            // mais aucune réponse n'est renvoyée — même en cas d'erreur (spec JSON-RPC 2.0).
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(httpStatusFor(response.error())).body(response);
    }

    private ResponseEntity<?> handleBatch(JsonNode arrayNode) {
        if (arrayNode.isEmpty()) {
            return invalidRequestResponse(null, "Batch request must not be empty");
        }

        List<JsonRpcResponse> responses = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (!item.isObject()) {
                responses.add(JsonRpcResponse.failure(
                        new JsonRpcError(JsonRpcError.INVALID_REQUEST, "Invalid Request",
                                "Each batch element must be a JSON object"),
                        null));
                continue;
            }
            JsonRpcRequest request = parseOrNull(item);
            if (request == null) {
                responses.add(JsonRpcResponse.failure(
                        new JsonRpcError(JsonRpcError.INVALID_REQUEST, "Invalid Request",
                                "Malformed request or missing/invalid 'jsonrpc' field"),
                        extractId(item)));
                continue;
            }
            JsonRpcResponse response = dispatchService.dispatch(request);
            if (request.id() != null) {
                // Une notification dans un batch n'a pas de réponse individuelle (spec).
                responses.add(response);
            }
        }

        if (responses.isEmpty()) {
            // Le batch ne contenait que des notifications.
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(responses);
    }

    /**
     * Parse un noeud JSON en {@link JsonRpcRequest}, en validant au passage le champ
     * {@code jsonrpc} et la présence de {@code method}. Retourne {@code null} si la requête
     * est structurellement invalide — jamais d'exception, l'appelant décide de la réponse.
     */
    private JsonRpcRequest parseOrNull(JsonNode node) {
        try {
            JsonRpcRequest request = objectMapper.convertValue(node, JsonRpcRequest.class);
            if (!"2.0".equals(request.jsonrpc()) || request.method() == null || request.method().isBlank()) {
                return null;
            }
            return request;
        } catch (Exception e) {
            log.debug("Requête JSON-RPC illisible : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrait au mieux le champ {@code id} d'un noeud potentiellement invalide, pour le
     * recopier dans la réponse d'erreur même quand le reste de la requête est illisible —
     * mieux qu'un {@code id} toujours {@code null} pour aider le client à corréler.
     */
    private Object extractId(JsonNode node) {
        if (node == null || !node.has("id")) {
            return null;
        }
        JsonNode idNode = node.get("id");
        if (idNode.isNumber()) {
            return idNode.asLong();
        }
        if (idNode.isString()) {
            return idNode.asString(null);
        }
        return null;
    }

    private ResponseEntity<?> invalidRequestResponse(Object id, String detail) {
        JsonRpcResponse response = JsonRpcResponse.failure(
                new JsonRpcError(JsonRpcError.INVALID_REQUEST, "Invalid Request", detail), id);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Associe un statut HTTP à une réponse JSON-RPC selon son code d'erreur.
     *
     * <p>{@code METHOD_NOT_FOUND} reste volontairement {@code 200} : c'est une réponse
     * protocolaire bien formée sur "cette méthode n'existe pas", pas une panne de
     * transport — comportement conforme à la lettre de la spec, et à la Javadoc historique
     * de cette classe. Les autres codes reflètent la nature réelle de l'erreur, alignés
     * sur les statuts que le même échec produirait côté REST.
     */
    private HttpStatus httpStatusFor(JsonRpcError error) {
        if (error == null) {
            return HttpStatus.OK;
        }
        return switch (error.code()) {
            case JsonRpcError.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case JsonRpcError.ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case JsonRpcError.CONFLICT -> HttpStatus.CONFLICT;
            case JsonRpcError.BUSINESS_ERROR, JsonRpcError.INVALID_PARAMS,
                 JsonRpcError.INVALID_REQUEST, JsonRpcError.PARSE_ERROR -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.OK;
        };
    }
}
