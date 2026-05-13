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

/**
 * Point d'entrée HTTP pour la gestion des tournois.
 */
@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final BracketService bracketService;

    public TournamentController(TournamentService tournamentService, BracketService bracketService) {
        this.tournamentService = tournamentService;
        this.bracketService = bracketService;
    }

    /**
     * Crée un nouveau tournoi au statut {@code OPEN}.
     *
     * @param req contient le nom et le nombre maximum de joueurs (puissance de 2)
     * @return {@code 201 Created} avec le tournoi créé
     */
    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(@Valid @RequestBody CreateTournamentRequest req) {
        TournamentResponse resp = tournamentService.createTournament(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Retourne la liste de tous les tournois.
     *
     * @return {@code 200 OK} avec la liste des tournois
     */
    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    /**
     * Retourne un tournoi par son identifiant.
     *
     * @param id identifiant du tournoi
     * @return {@code 200 OK} avec le tournoi
     */
    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    /**
     * Démarre un tournoi : génère le bracket du premier tour
     * et passe le statut à {@code IN_PROGRESS}.
     *
     * <p>Les joueurs sont répartis aléatoirement. Si le nombre d'inscrits
     * est impair, le dernier joueur reçoit un bye (qualification automatique).
     *
     * @param id identifiant du tournoi
     * @return {@code 200 OK}
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startTournament(@PathVariable Long id) {
        bracketService.startTournament(id);
        return ResponseEntity.ok().build();
    }
}
