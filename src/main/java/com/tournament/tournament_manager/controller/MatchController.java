package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.domain.port.in.GetMatchUseCase;
import com.tournament.tournament_manager.domain.port.in.RecordMatchResultUseCase;
import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final RecordMatchResultUseCase recordMatchResultUseCase;
    private final GetMatchUseCase getMatchUseCase;

    public MatchController(RecordMatchResultUseCase recordMatchResultUseCase,
                           GetMatchUseCase getMatchUseCase) {
        this.recordMatchResultUseCase = recordMatchResultUseCase;
        this.getMatchUseCase = getMatchUseCase;
    }

    @PutMapping("/{id}/result")
    public ResponseEntity<MatchResponse> recordMatchResult(
            @PathVariable Long id,
            @Valid @RequestBody RecordMatchResultRequest request) {
        return ResponseEntity.ok(recordMatchResultUseCase.recordMatchResult(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable Long id) {
        return ResponseEntity.ok(getMatchUseCase.getMatchById(id));
    }
}