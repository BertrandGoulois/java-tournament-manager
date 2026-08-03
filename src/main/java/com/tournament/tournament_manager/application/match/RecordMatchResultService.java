package com.tournament.tournament_manager.application.match;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.in.match.RecordMatchResultUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.match.PublishMatchEventPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.match.MatchResponse;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cas d'utilisation : enregistrement du résultat d'un match.
 */
@Slf4j
@Service
@Transactional
public class RecordMatchResultService implements RecordMatchResultUseCase {

    private final LoadMatchPort loadMatchPort;
    private final SaveMatchPort saveMatchPort;
    private final PublishMatchEventPort publishMatchEventPort;
    private final Counter matchResultRecordedCounter;

    public RecordMatchResultService(LoadMatchPort loadMatchPort,
                                    SaveMatchPort saveMatchPort,
                                    PublishMatchEventPort publishMatchEventPort,
                                    MeterRegistry meterRegistry) {
        this.loadMatchPort = loadMatchPort;
        this.saveMatchPort = saveMatchPort;
        this.publishMatchEventPort = publishMatchEventPort;
        this.matchResultRecordedCounter = Counter.builder("match.result.recorded")
                .description("Nombre de résultats de match enregistrés")
                .register(meterRegistry);
    }

    @Override
    public MatchResponse recordMatchResult(Long matchId, RecordMatchResultRequest request) {
        Match match = loadMatchPort.loadMatch(matchId);

        if (match.getStatus() == MatchStatus.FINISHED) {
            log.warn("Tentative d'enregistrement d'un résultat sur un match déjà terminé [matchId={}]", matchId);
            throw new InvalidException("Match already finished");
        }

        if (!request.winnerId().equals(match.getPlayer1().getId()) &&
                (match.getPlayer2() == null || !request.winnerId().equals(match.getPlayer2().getId()))) {
            log.warn("Vainqueur invalide [matchId={}, winnerId={}, player1={}, player2={}]",
                    matchId, request.winnerId(),
                    match.getPlayer1().getId(),
                    match.getPlayer2() != null ? match.getPlayer2().getId() : null);
            throw new InvalidException("Winner is not a player of this match");
        }

        match.setWinner(match.getPlayer1().getId().equals(request.winnerId())
                ? match.getPlayer1()
                : match.getPlayer2());
        match.setStatus(MatchStatus.FINISHED);
        match.setPlayedAt(LocalDateTime.now());

        Match saved = saveMatchPort.saveMatch(match);
        publishMatchEventPort.publishMatchFinished(
                new MatchFinishedEvent(saved.getId()), saved.getTournament().getId());

        log.info("Résultat enregistré [matchId={}, tournamentId={}, winner='{}']",
                saved.getId(),
                saved.getTournament().getId(),
                saved.getWinner().getUsername());

        matchResultRecordedCounter.increment();
        return toResponse(saved);
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