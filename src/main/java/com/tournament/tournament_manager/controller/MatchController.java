package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.domain.port.in.match.GetMatchCommentaryUseCase;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchUseCase;
import com.tournament.tournament_manager.domain.port.in.match.RecordMatchResultUseCase;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.match.MatchCommentaryResponse;
import com.tournament.tournament_manager.dto.response.match.MatchResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée HTTP pour la gestion des matchs.
 * Délègue aux ports entrants {@link RecordMatchResultUseCase} et {@link GetMatchUseCase}.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final RecordMatchResultUseCase recordMatchResultUseCase;
    private final GetMatchUseCase getMatchUseCase;
    private final GetMatchCommentaryUseCase getMatchCommentaryUseCase;

    public MatchController(RecordMatchResultUseCase recordMatchResultUseCase,
                           GetMatchUseCase getMatchUseCase,
                           GetMatchCommentaryUseCase getMatchCommentaryUseCase) {
        this.recordMatchResultUseCase = recordMatchResultUseCase;
        this.getMatchUseCase = getMatchUseCase;
        this.getMatchCommentaryUseCase = getMatchCommentaryUseCase;
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
    public ResponseEntity<MatchResponse> recordMatchResult(
            @PathVariable Long id,
            @Valid @RequestBody RecordMatchResultRequest request) {
        return ResponseEntity.ok(recordMatchResultUseCase.recordMatchResult(id, request));
    }

    /**
     * Retourne un match par son identifiant.
     *
     * @param id identifiant du match
     * @return {@code 200 OK} avec le match
     */
    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable Long id) {
        return ResponseEntity.ok(getMatchUseCase.getMatchById(id));
    }

    /**
     * Retourne le commentaire narratif d'un match généré par LLM.
     * Le commentaire est généré de façon asynchrone après la fin du match.
     *
     * @param id identifiant du match
     * @return {@code 200 OK} avec le commentaire
     */
    @GetMapping("/{id}/commentary")
    public ResponseEntity<MatchCommentaryResponse> getMatchCommentary(@PathVariable Long id) {
        return ResponseEntity.ok(getMatchCommentaryUseCase.getMatchCommentary(id));
    }
}