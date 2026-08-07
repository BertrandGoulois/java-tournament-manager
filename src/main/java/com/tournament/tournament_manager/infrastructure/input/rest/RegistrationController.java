package com.tournament.tournament_manager.infrastructure.input.rest;

import com.tournament.tournament_manager.domain.port.in.registration.GetRegistrationsUseCase;
import com.tournament.tournament_manager.domain.port.in.registration.RegisterPlayerUseCase;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.registration.RegistrationResponse;
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

import java.util.List;

/**
 * Point d'entrée HTTP pour la gestion des inscriptions aux tournois.
 */
@RestController
@RequestMapping("/api/registrations")
@Tag(name = "Registrations", description = "Gestion des inscriptions aux tournois")
public class RegistrationController {

    private final RegisterPlayerUseCase registerPlayerUseCase;
    private final GetRegistrationsUseCase getRegistrationsUseCase;

    public RegistrationController(RegisterPlayerUseCase registerPlayerUseCase,
                                  GetRegistrationsUseCase getRegistrationsUseCase) {
        this.registerPlayerUseCase = registerPlayerUseCase;
        this.getRegistrationsUseCase = getRegistrationsUseCase;
    }

    @Operation(summary = "Inscrire un joueur à un tournoi",
            description = "Inscrit un joueur à un tournoi ouvert. Le tournoi doit être en statut OPEN et ne pas être complet.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscription créée"),
            @ApiResponse(responseCode = "400", description = "Tournoi non ouvert, joueur déjà inscrit ou tournoi complet",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Joueur ou tournoi introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RegistrationResponse> createRegistration(
            @Valid @RequestBody CreateRegistrationRequest req) {
        return ResponseEntity.status(201).body(registerPlayerUseCase.registerPlayer(req));
    }

    @Operation(summary = "Lister les inscriptions d'un tournoi",
            description = "Retourne la liste des joueurs inscrits à un tournoi donné.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des inscriptions"),
            @ApiResponse(responseCode = "404", description = "Tournoi introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{tournamentId}")
    public ResponseEntity<Page<RegistrationResponse>> getTournamentRegistrations(
            @Parameter(description = "ID du tournoi") @PathVariable Long tournamentId, Pageable pageable) {
        var result = getRegistrationsUseCase.getTournamentRegistrations(tournamentId,
                new com.tournament.tournament_manager.domain.model.PageRequest(pageable.getPageNumber(), pageable.getPageSize()));
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(
                result.content(), pageable, result.totalElements()));
    }
}