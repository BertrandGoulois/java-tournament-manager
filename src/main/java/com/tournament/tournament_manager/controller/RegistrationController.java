package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.dto.request.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.RegistrationResponse;
import com.tournament.tournament_manager.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService){
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> createRegistration(@Valid @RequestBody CreateRegistrationRequest req){
        RegistrationResponse resp = registrationService.registerPlayer(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{tournamentId}")
    public ResponseEntity<List<RegistrationResponse>> getTournamentregistrations(@PathVariable Long tournamentId){
        return ResponseEntity.ok(registrationService.getTournamentRegistrations(tournamentId));
    }
}
