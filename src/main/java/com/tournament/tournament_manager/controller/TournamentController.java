package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.domain.port.in.CreateTournamentUseCase;
import com.tournament.tournament_manager.domain.port.in.GetTournamentUseCase;
import com.tournament.tournament_manager.domain.port.in.StartTournamentUseCase;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.TournamentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Point d'entrée HTTP pour la gestion des tournois.
 * Délègue aux ports entrants {@link CreateTournamentUseCase},
 * {@link GetTournamentUseCase} et {@link StartTournamentUseCase}.
 */
@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final CreateTournamentUseCase createTournamentUseCase;
    private final GetTournamentUseCase getTournamentUseCase;
    private final StartTournamentUseCase startTournamentUseCase;

    public TournamentController(CreateTournamentUseCase createTournamentUseCase,
                                GetTournamentUseCase getTournamentUseCase,
                                StartTournamentUseCase startTournamentUseCase) {
        this.createTournamentUseCase = createTournamentUseCase;
        this.getTournamentUseCase = getTournamentUseCase;
        this.startTournamentUseCase = startTournamentUseCase;
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody CreateTournamentRequest req) {
        return ResponseEntity.status(201).body(createTournamentUseCase.createTournament(req));
    }

    @GetMapping
    public ResponseEntity<Page<TournamentResponse>> getAllTournaments(Pageable pageable) {
        return ResponseEntity.ok(getTournamentUseCase.getAllTournaments(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        return ResponseEntity.ok(getTournamentUseCase.getTournamentById(id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startTournament(@PathVariable Long id) {
        startTournamentUseCase.startTournament(id);
        return ResponseEntity.ok().build();
    }
}