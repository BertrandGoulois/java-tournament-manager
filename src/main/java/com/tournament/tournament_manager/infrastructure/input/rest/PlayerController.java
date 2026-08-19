package com.tournament.tournament_manager.infrastructure.input.rest;

import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.domain.port.in.player.DeletePlayerUseCase;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerStatsUseCase;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.dto.response.player.PlayerStatsResponse;
import com.tournament.tournament_manager.exception.handler.ErrorResponse;
import com.tournament.tournament_manager.infrastructure.input.mapper.PlayerRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée HTTP pour la gestion des joueurs.
 *
 * <p>Convertit entre les DTO REST (Swagger, validation Jakarta) et le domaine pur via
 * {@link PlayerRestMapper} — les ports du domaine ({@code GetPlayerUseCase} etc.) ne
 * connaissent plus ces DTO (voir point 22 de la revue).
 */
@RestController
@RequestMapping("/api/players")
@Tag(name = "Players", description = "Gestion des joueurs (création, statistiques, suppression)")
public class PlayerController {

    private final CreatePlayerUseCase createPlayerUseCase;
    private final GetPlayerUseCase getPlayerUseCase;
    private final GetPlayerStatsUseCase getPlayerStatsUseCase;
    private final DeletePlayerUseCase deletePlayerUseCase;
    private final PlayerRestMapper playerRestMapper;

    public PlayerController(CreatePlayerUseCase createPlayerUseCase,
                            GetPlayerUseCase getPlayerUseCase,
                            GetPlayerStatsUseCase getPlayerStatsUseCase,
                            DeletePlayerUseCase deletePlayerUseCase,
                            PlayerRestMapper playerRestMapper) {
        this.createPlayerUseCase = createPlayerUseCase;
        this.getPlayerUseCase = getPlayerUseCase;
        this.getPlayerStatsUseCase = getPlayerStatsUseCase;
        this.deletePlayerUseCase = deletePlayerUseCase;
        this.playerRestMapper = playerRestMapper;
    }

    @Operation(summary = "Créer un joueur",
            description = "Crée un nouveau joueur avec un ELO de départ à 1000. Réservé aux ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Joueur créé"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username ou email déjà utilisé",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(
            @Valid @RequestBody CreatePlayerRequest req) {
        var player = createPlayerUseCase.createPlayer(playerRestMapper.toCommand(req));
        return ResponseEntity.status(201).body(playerRestMapper.toResponse(player));
    }

    @Operation(summary = "Lister les joueurs", description = "Retourne la liste paginée des joueurs.")
    @ApiResponse(responseCode = "200", description = "Liste des joueurs")
    @GetMapping
    public ResponseEntity<Page<PlayerResponse>> getAllPlayers(Pageable pageable) {
        var result = getPlayerUseCase.getAllPlayers(
                new com.tournament.tournament_manager.domain.model.PageRequest(pageable.getPageNumber(), pageable.getPageSize()));
        var content = result.content().stream().map(playerRestMapper::toResponse).toList();
        return ResponseEntity.ok(new PageImpl<>(content, pageable, result.totalElements()));
    }

    @Operation(summary = "Obtenir un joueur par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Joueur trouvé"),
            @ApiResponse(responseCode = "404", description = "Joueur introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(
            @Parameter(description = "ID du joueur") @PathVariable Long id) {
        return ResponseEntity.ok(playerRestMapper.toResponse(getPlayerUseCase.getPlayerById(id)));
    }

    @Operation(summary = "Obtenir les statistiques d'un joueur",
            description = "Retourne le rating ELO, le nombre de matchs joués, le win rate et l'historique ELO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistiques du joueur"),
            @ApiResponse(responseCode = "404", description = "Joueur introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/stats")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(
            @Parameter(description = "ID du joueur") @PathVariable Long id) {
        return ResponseEntity.ok(playerRestMapper.toStatsResponse(getPlayerStatsUseCase.getPlayerStats(id)));
    }

    @Operation(summary = "Supprimer un joueur (soft delete)",
            description = "Marque le joueur comme supprimé sans le retirer physiquement de la base. Réservé aux ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Joueur supprimé"),
            @ApiResponse(responseCode = "404", description = "Joueur introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(
            @Parameter(description = "ID du joueur") @PathVariable Long id) {
        deletePlayerUseCase.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
