package com.tournament.tournament_manager.application.match;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.RecordMatchResultCommand;
import com.tournament.tournament_manager.domain.port.in.match.RecordMatchResultUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.match.PublishMatchEventPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : enregistrement du résultat d'un match. Retourne un objet de domaine
 * pur — voir la Javadoc de {@code GetPlayerService}.
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
    public Match recordMatchResult(Long matchId, RecordMatchResultCommand command) {
        Match match = loadMatchPort.loadMatch(matchId);

        // Les deux règles (match pas déjà terminé, vainqueur parmi les deux participants)
        // sont désormais protégées par Match.recordResult lui-même - voir sa Javadoc.
        match.recordResult(command.winnerId());

        Match saved = saveMatchPort.saveMatch(match);
        int player2EloBefore = saved.getPlayer2() != null ? saved.getPlayer2().getEloRating().value() : 0;
        publishMatchEventPort.publishMatchFinished(
                new MatchFinishedEvent(
                        saved.getId(),
                        saved.getPlayer1().getEloRating().value(),
                        player2EloBefore),
                saved.getTournament().getId());

        log.info("Résultat enregistré [matchId={}, tournamentId={}, winner='{}']",
                saved.getId(),
                saved.getTournament().getId(),
                saved.getWinner().getUsername());

        matchResultRecordedCounter.increment();
        return saved;
    }
}
