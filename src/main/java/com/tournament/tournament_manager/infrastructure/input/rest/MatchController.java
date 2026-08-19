package com.tournament.tournament_manager.infrastructure.input.rest;

import com.tournament.tournament_manager.domain.port.in.match.GetMatchCommentaryUseCase;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchUseCase;
import com.tournament.tournament_manager.domain.port.in.match.RecordMatchResultUseCase;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.match.MatchCommentaryResponse;
import com.tournament.tournament_manager.dto.response.match.MatchResponse;
import com.tournament.tournament_manager.exception.handler.ErrorResponse;
import com.tournament.tournament_manager.infrastructure.input.mapper.MatchRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée HTTP pour la gestion des matchs.
 *
 * <p>Convertit entre les DTO REST et le domaine pur via {@link MatchRestMapper} — voir la
 * Javadoc de {@code PlayerController}.
 */
@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches", description = "Gestion des matchs (résultats, commentaires)")
public class MatchController {

    private final RecordMatchResultUseCase recordMatchResultUseCase;
    private final GetMatchUseCase getMatchUseCase;
    private final GetMatchCommentaryUseCase getMatchCommentaryUseCase;
    private final MatchRestMapper matchRestMapper;

    public MatchController(RecordMatchResultUseCase recordMatchResultUseCase,
                           GetMatchUseCase getMatchUseCase,
                           GetMatchCommentaryUseCase getMatchCommentaryUseCase,
                           MatchRestMapper matchRestMapper) {
        this.recordMatchResultUseCase = recordMatchResultUseCase;
        this.getMatchUseCase = getMatchUseCase;
        this.getMatchCommentaryUseCase = getMatchCommentaryUseCase;
        this.matchRestMapper = matchRestMapper;
    }

    @Operation(summary = "Enregistrer le résultat d'un match",
            description = "Enregistre le vainqueur et déclenche la chaîne d'événements Kafka : " +
                    "mise à jour ELO, progression du tournoi, notification WebSocket, génération de commentaire LLM. Réservé aux ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Résultat enregistré"),
            @ApiResponse(responseCode = "400", description = "Match déjà terminé ou vainqueur invalide",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Match introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/result")
    public ResponseEntity<MatchResponse> recordMatchResult(
            @Parameter(description = "ID du match") @PathVariable Long id,
            @Valid @RequestBody RecordMatchResultRequest request) {
        var match = recordMatchResultUseCase.recordMatchResult(id, matchRestMapper.toCommand(request));
        return ResponseEntity.ok(matchRestMapper.toResponse(match));
    }

    @Operation(summary = "Obtenir un match par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match trouvé"),
            @ApiResponse(responseCode = "404", description = "Match introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatchById(
            @Parameter(description = "ID du match") @PathVariable Long id) {
        return ResponseEntity.ok(matchRestMapper.toResponse(getMatchUseCase.getMatchById(id)));
    }

    @Operation(summary = "Obtenir le commentaire d'un match",
            description = "Retourne le commentaire narratif généré de façon asynchrone par OpenAI GPT-4o-mini après la fin du match. " +
                    "Retourne 'Commentaire en cours de génération...' si le LLM n'a pas encore répondu.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commentaire du match"),
            @ApiResponse(responseCode = "404", description = "Match introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/commentary")
    public ResponseEntity<MatchCommentaryResponse> getMatchCommentary(
            @Parameter(description = "ID du match") @PathVariable Long id) {
        return ResponseEntity.ok(matchRestMapper.toResponse(getMatchCommentaryUseCase.getMatchCommentary(id)));
    }
}
