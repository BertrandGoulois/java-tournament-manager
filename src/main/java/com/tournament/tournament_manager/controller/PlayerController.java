package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.dto.response.PlayerStatsResponse;
import com.tournament.tournament_manager.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Point d'entrée HTTP pour la gestion des joueurs.
 */
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    /**
     * Crée un nouveau joueur.
     *
     * @param req contient le username et l'email
     * @return {@code 201 Created} avec le joueur créé
     */
    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(@Valid @RequestBody CreatePlayerRequest req){
        PlayerResponse resp = playerService.createPlayer(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * Retourne la liste de tous les joueurs.
     *
     * @return {@code 200 OK} avec la liste des joueurs
     */
    @GetMapping
    public ResponseEntity<List<PlayerResponse>> getAllPlayers(){
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    /**
     * Retourne un joueur par son identifiant.
     *
     * @param id identifiant du joueur
     * @return {@code 200 OK} avec le joueur
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getPlayerById(id));
    }

    /**
     * Retourne les statistiques d'un joueur : matchs joués, victoires,
     * défaites, win rate et historique ELO.
     *
     * <p>Résultat mis en cache Redis — invalidé automatiquement après chaque match.
     *
     * @param id identifiant du joueur
     * @return {@code 200 OK} avec les statistiques du joueur
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getPlayerStats(id));
    }
}
