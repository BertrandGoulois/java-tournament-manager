package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.dto.request.rpc.JsonRpcRequest;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import com.tournament.tournament_manager.service.rpc.JsonRpcDispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée unique pour les requêtes JSON-RPC 2.0.
 *
 * <p>Coexiste avec l'API REST existante ({@code /api/tournaments}, {@code /api/matches}...) :
 * l'authentification reste exclusivement gérée par {@code /api/auth/**}, tandis que ce contrôleur
 * expose les opérations métier sous forme de méthodes nommées (ex. {@code tournament.create}).
 */
@RestController
@RequestMapping("/api/rpc")
public class JsonRpcController {

    private final JsonRpcDispatchService dispatchService;

    public JsonRpcController(JsonRpcDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping
    public ResponseEntity<JsonRpcResponse> handle(@RequestBody JsonRpcRequest request) {
        return ResponseEntity.ok(dispatchService.dispatch(request));
    }
}