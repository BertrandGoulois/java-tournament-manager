package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.TournamentResponse;
import com.tournament.tournament_manager.service.BracketService;
import com.tournament.tournament_manager.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final BracketService bracketService;

    public TournamentController(TournamentService tournamentService, BracketService bracketService) {
        this.tournamentService = tournamentService;
        this.bracketService = bracketService;
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(@Valid @RequestBody CreateTournamentRequest req) {
        TournamentResponse resp = tournamentService.createTournament(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startTournament(@PathVariable Long id) {
        bracketService.startTournament(id);
        return ResponseEntity.ok().build();
    }
}
