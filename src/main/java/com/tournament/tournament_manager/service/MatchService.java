package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final BracketService bracketService;

    public MatchService(MatchRepository matchRepository, BracketService bracketService) {
        this.matchRepository = matchRepository;
        this.bracketService = bracketService;
    }

    @Transactional
    public MatchResponse recordMatchResult(Long matchId, RecordMatchResultRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        if (match.getStatus() != MatchStatus.PENDING) {
            throw new InvalidException("Match is already finished");
        }
        Set<Long> validPlayerIds = Set.of(match.getPlayer1().getId(), match.getPlayer2().getId());
        if (!validPlayerIds.contains(request.winnerId())) {
            throw new InvalidException("Winner must be one of the match players");
        }
        Player winner = match.getPlayer1().getId().equals(request.winnerId())
                ? match.getPlayer1()
                : match.getPlayer2();
        match.setStatus(MatchStatus.FINISHED);
        match.setWinner(winner);
        match.setPlayedAt(LocalDateTime.now());
        Match saved = matchRepository.save(match);
        bracketService.advanceToNextRound(saved.getTournament(), saved.getRound());
        return toResponse(saved);
    }

    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException(id));
        return toResponse(match);
    }

    private MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getRound(),
                match.getStatus(),
                match.getPlayedAt(),
                match.getTournament().getId(),
                match.getPlayer1().getId(),
                match.getPlayer2() != null ? match.getPlayer2().getId() : null,
                match.getWinner() != null ? match.getWinner().getId() : null
        );
    }
}
