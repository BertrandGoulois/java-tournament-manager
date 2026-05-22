package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.domain.port.in.registration.GetRegistrationsUseCase;
import com.tournament.tournament_manager.domain.port.in.registration.RegisterPlayerUseCase;
import com.tournament.tournament_manager.dto.request.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.RegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Point d'entrée HTTP pour la gestion des inscriptions aux tournois.
 * Délègue aux ports entrants {@link RegisterPlayerUseCase} et {@link GetRegistrationsUseCase}.
 */
@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegisterPlayerUseCase registerPlayerUseCase;
    private final GetRegistrationsUseCase getRegistrationsUseCase;

    public RegistrationController(RegisterPlayerUseCase registerPlayerUseCase,
                                  GetRegistrationsUseCase getRegistrationsUseCase) {
        this.registerPlayerUseCase = registerPlayerUseCase;
        this.getRegistrationsUseCase = getRegistrationsUseCase;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> createRegistration(
            @Valid @RequestBody CreateRegistrationRequest req) {
        return ResponseEntity.status(201).body(registerPlayerUseCase.registerPlayer(req));
    }

    @GetMapping("/{tournamentId}")
    public ResponseEntity<List<RegistrationResponse>> getTournamentRegistrations(
            @PathVariable Long tournamentId) {
        return ResponseEntity.ok(getRegistrationsUseCase.getTournamentRegistrations(tournamentId));
    }
}