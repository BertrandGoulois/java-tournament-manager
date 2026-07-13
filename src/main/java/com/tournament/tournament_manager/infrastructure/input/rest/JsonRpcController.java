package com.tournament.tournament_manager.infrastructure.input.rest;

import com.tournament.tournament_manager.application.rpc.JsonRpcDispatchService;
import com.tournament.tournament_manager.dto.request.rpc.JsonRpcRequest;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée unique pour les requêtes JSON-RPC 2.0.
 *
 * <p>Coexiste avec l'API REST existante. L'authentification reste exclusivement
 * gérée par {@code /api/auth/**}. Réservé aux ADMIN.
 */
@RestController
@RequestMapping("/api/rpc")
@Tag(name = "JSON-RPC", description = "API JSON-RPC 2.0 - endpoint unique exposant toutes les opérations métier")
public class JsonRpcController {

    private final JsonRpcDispatchService dispatchService;

    public JsonRpcController(JsonRpcDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Operation(summary = "Exécuter une méthode JSON-RPC",
            description = """
                    Endpoint unique JSON-RPC 2.0. Le champ `method` détermine l'opération à exécuter.
                    
                    Méthodes disponibles :
                    - `tournament.create`, `tournament.start`, `tournament.getById`, `tournament.getAll`, `tournament.delete`, `tournament.getBracket`, `tournament.getStandings`
                    - `player.create`, `player.getById`, `player.getAll`, `player.getStats`, `player.delete`
                    - `registration.register`, `registration.getByTournament`
                    - `match.getById`, `match.recordResult`, `match.getCommentary`
                    
                    Le statut HTTP est toujours `200` même en cas d'erreur applicative (spec JSON-RPC 2.0).
                    Codes d'erreur : `-32601` méthode inconnue, `-32602` paramètres invalides, `-32603` erreur interne.
                    """)
    @ApiResponse(responseCode = "200", description = "Réponse JSON-RPC (succès ou erreur applicative)",
            content = @Content(schema = @Schema(implementation = JsonRpcResponse.class)))
    @PostMapping
    public ResponseEntity<JsonRpcResponse> handle(@RequestBody JsonRpcRequest request) {
        return ResponseEntity.ok(dispatchService.dispatch(request));
    }
}