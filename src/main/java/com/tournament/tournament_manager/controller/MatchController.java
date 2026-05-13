package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée HTTP pour la gestion des matchs.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService){
        this.matchService = matchService;
    }

    /**
     * Enregistre le résultat d'un match et déclenche la chaîne Kafka
     * (mise à jour ELO, avancement du bracket, notification WebSocket).
     *
     * @param id      identifiant du match
     * @param request contient l'identifiant du vainqueur
     * @return {@code 200 OK} avec le match mis à jour
     */
    @PutMapping("/{id}/result")
    public ResponseEntity<MatchResponse> recordMatchResult(@PathVariable Long id, @Valid @RequestBody RecordMatchResultRequest request){
        return ResponseEntity.ok(matchService.recordMatchResult(id, request));
    }

    /**
     * Retourne un match par son identifiant.
     *
     * @param id identifiant du match
     * @return {@code 200 OK} avec le match
     */
    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable Long id){
        return ResponseEntity.ok(matchService.getMatchById(id));
    }
}
