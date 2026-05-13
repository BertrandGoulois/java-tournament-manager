package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.in.GetMatchUseCase;
import com.tournament.tournament_manager.domain.port.in.RecordMatchResultUseCase;
import com.tournament.tournament_manager.domain.port.out.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.PublishMatchEventPort;
import com.tournament.tournament_manager.domain.port.out.SaveMatchPort;
import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MatchService implements RecordMatchResultUseCase, GetMatchUseCase {

    private final LoadMatchPort loadMatchPort;
    private final SaveMatchPort saveMatchPort;
    private final PublishMatchEventPort publishMatchEventPort;

    public MatchService(LoadMatchPort loadMatchPort,
                        SaveMatchPort saveMatchPort,
                        PublishMatchEventPort publishMatchEventPort) {
        this.loadMatchPort = loadMatchPort;
        this.saveMatchPort = saveMatchPort;
        this.publishMatchEventPort = publishMatchEventPort;
    }

    @Override
    @Transactional
    public MatchResponse recordMatchResult(Long matchId, RecordMatchResultRequest request) {
        Match match = loadMatchPort.loadMatch(matchId);

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new InvalidException("Match already finished");
        }

        if (!request.winnerId().equals(match.getPlayer1().getId()) &&
                !request.winnerId().equals(match.getPlayer2().getId())) {
            throw new InvalidException("Winner is not a player of this match");
        }

        match.setWinner(match.getPlayer1().getId().equals(request.winnerId())
                ? match.getPlayer1()
                : match.getPlayer2());
        match.setStatus(MatchStatus.FINISHED);

        Match saved = saveMatchPort.saveMatch(match);
        publishMatchEventPort.publishMatchFinished(new MatchFinishedEvent(saved.getId()));

        return toResponse(saved);
    }

    @Override
    public MatchResponse getMatchById(Long id) {
        return toResponse(loadMatchPort.loadMatch(id));
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