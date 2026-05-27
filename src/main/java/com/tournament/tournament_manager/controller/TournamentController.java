package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.domain.port.in.tournament.CreateTournamentUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.GetBracketUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.GetTournamentUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.StartTournamentUseCase;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.BracketResponse;
import com.tournament.tournament_manager.dto.response.TournamentResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final GetBracketUseCase getBracketUseCase;

    public TournamentController(CreateTournamentUseCase createTournamentUseCase,
                                GetTournamentUseCase getTournamentUseCase,
                                StartTournamentUseCase startTournamentUseCase,
                                GetBracketUseCase getBracketUseCase) {
        this.createTournamentUseCase = createTournamentUseCase;
        this.getTournamentUseCase = getTournamentUseCase;
        this.startTournamentUseCase = startTournamentUseCase;
        this.getBracketUseCase = getBracketUseCase;
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

    @GetMapping("/{id}/bracket")
    public ResponseEntity<BracketResponse> getBracket(@PathVariable Long id) {
        return ResponseEntity.ok(getBracketUseCase.getBracket(id));
    }
}