package com.tournament.tournament_manager.infrastructure.input.rest;

import com.tournament.tournament_manager.domain.port.in.tournament.*;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.tournament.BracketResponse;
import com.tournament.tournament_manager.dto.response.tournament.StandingsResponse;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import com.tournament.tournament_manager.exception.handler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée HTTP pour la gestion des tournois.
 */
@RestController
@RequestMapping("/api/tournaments")
@Tag(name = "Tournaments", description = "Gestion des tournois (création, démarrage, bracket, classement)")
public class TournamentController {

    private final CreateTournamentUseCase createTournamentUseCase;
    private final GetTournamentUseCase getTournamentUseCase;
    private final StartTournamentUseCase startTournamentUseCase;
    private final GetBracketUseCase getBracketUseCase;
    private final DeleteTournamentUseCase deleteTournamentUseCase;
    private final GetStandingsUseCase getStandingsUseCase;

    public TournamentController(CreateTournamentUseCase createTournamentUseCase,
                                GetTournamentUseCase getTournamentUseCase,
                                StartTournamentUseCase startTournamentUseCase,
                                GetBracketUseCase getBracketUseCase,
                                DeleteTournamentUseCase deleteTournamentUseCase,
                                GetStandingsUseCase getStandingsUseCase) {
        this.createTournamentUseCase = createTournamentUseCase;
        this.getTournamentUseCase = getTournamentUseCase;
        this.startTournamentUseCase = startTournamentUseCase;
        this.getBracketUseCase = getBracketUseCase;
        this.deleteTournamentUseCase = deleteTournamentUseCase;
        this.getStandingsUseCase = getStandingsUseCase;
    }

    @Operation(summary = "Créer un tournoi",
            description = "Crée un nouveau tournoi. Le format est optionnel (SINGLE_ELIMINATION par défaut). " +
                    "Pour GROUPS_THEN_KNOCKOUT, numberOfGroups et qualifiersPerGroup sont requis. Réservé aux ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tournoi créé"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nom de tournoi déjà utilisé",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody CreateTournamentRequest req) {
        return ResponseEntity.status(201).body(createTournamentUseCase.createTournament(req));
    }

    @Operation(summary = "Lister les tournois", description = "Retourne la liste paginée des tournois.")
    @ApiResponse(responseCode = "200", description = "Liste des tournois")
    @GetMapping
    public ResponseEntity<Page<TournamentResponse>> getAllTournaments(Pageable pageable) {
        var result = getTournamentUseCase.getAllTournaments(
                new com.tournament.tournament_manager.domain.model.PageRequest(pageable.getPageNumber(), pageable.getPageSize()));
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(
                result.content(), pageable, result.totalElements()));
    }

    @Operation(summary = "Obtenir un tournoi par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournoi trouvé"),
            @ApiResponse(responseCode = "404", description = "Tournoi introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(
            @Parameter(description = "ID du tournoi") @PathVariable Long id) {
        return ResponseEntity.ok(getTournamentUseCase.getTournamentById(id));
    }

    @Operation(summary = "Démarrer un tournoi",
            description = "Génère les matchs initiaux selon le format du tournoi. Réservé aux ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournoi démarré"),
            @ApiResponse(responseCode = "400", description = "Tournoi non ouvert ou moins de 2 joueurs",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tournoi introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startTournament(
            @Parameter(description = "ID du tournoi") @PathVariable Long id) {
        startTournamentUseCase.startTournament(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Obtenir le bracket d'un tournoi",
            description = "Retourne les rounds et matchs du bracket. Pertinent pour SINGLE_ELIMINATION et la phase finale de GROUPS_THEN_KNOCKOUT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bracket du tournoi"),
            @ApiResponse(responseCode = "404", description = "Tournoi introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/bracket")
    public ResponseEntity<BracketResponse> getBracket(
            @Parameter(description = "ID du tournoi") @PathVariable Long id) {
        return ResponseEntity.ok(getBracketUseCase.getBracket(id));
    }

    @Operation(summary = "Obtenir le classement d'un tournoi",
            description = "Retourne le classement calculé à la demande. Pertinent pour ROUND_ROBIN (3 points par victoire).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classement du tournoi"),
            @ApiResponse(responseCode = "404", description = "Tournoi introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/standings")
    public ResponseEntity<StandingsResponse> getStandings(
            @Parameter(description = "ID du tournoi") @PathVariable Long id) {
        return ResponseEntity.ok(getStandingsUseCase.getStandings(id));
    }

    @Operation(summary = "Supprimer un tournoi (soft delete)",
            description = "Marque le tournoi comme supprimé sans le retirer physiquement de la base. Réservé aux ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tournoi supprimé"),
            @ApiResponse(responseCode = "404", description = "Tournoi introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(
            @Parameter(description = "ID du tournoi") @PathVariable Long id) {
        deleteTournamentUseCase.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }
}