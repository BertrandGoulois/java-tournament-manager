package com.tournament.tournament_manager.infrastructure.input.rest;

import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.domain.port.in.player.DeletePlayerUseCase;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerStatsUseCase;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.dto.response.player.PlayerStatsResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée HTTP pour la gestion des joueurs.
 * Délègue aux ports entrants {@link CreatePlayerUseCase},
 * {@link GetPlayerUseCase} et {@link GetPlayerStatsUseCase}.
 */
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final CreatePlayerUseCase createPlayerUseCase;
    private final GetPlayerUseCase getPlayerUseCase;
    private final GetPlayerStatsUseCase getPlayerStatsUseCase;
    private final DeletePlayerUseCase deletePlayerUseCase;

    public PlayerController(CreatePlayerUseCase createPlayerUseCase,
                            GetPlayerUseCase getPlayerUseCase,
                            GetPlayerStatsUseCase getPlayerStatsUseCase,
                            DeletePlayerUseCase deletePlayerUseCase) {
        this.createPlayerUseCase = createPlayerUseCase;
        this.getPlayerUseCase = getPlayerUseCase;
        this.getPlayerStatsUseCase = getPlayerStatsUseCase;
        this.deletePlayerUseCase = deletePlayerUseCase;
    }

    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(
            @Valid @RequestBody CreatePlayerRequest req) {
        return ResponseEntity.status(201).body(createPlayerUseCase.createPlayer(req));
    }

    @GetMapping
    public ResponseEntity<Page<PlayerResponse>> getAllPlayers(Pageable pageable) {
        return ResponseEntity.ok(getPlayerUseCase.getAllPlayers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable Long id) {
        return ResponseEntity.ok(getPlayerUseCase.getPlayerById(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(@PathVariable Long id) {
        return ResponseEntity.ok(getPlayerStatsUseCase.getPlayerStats(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        deletePlayerUseCase.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}