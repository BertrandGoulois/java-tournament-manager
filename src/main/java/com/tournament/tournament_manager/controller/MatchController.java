package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService){
        this.matchService = matchService;
    }

    @PutMapping("/{id}/result")
    public ResponseEntity<MatchResponse> recordMatchResult(@PathVariable Long id, @Valid @RequestBody RecordMatchResultRequest request){
        return ResponseEntity.ok(matchService.recordMatchResult(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatchById(@PathVariable Long id){
        return ResponseEntity.ok(matchService.getMatchById(id));
    }
}
