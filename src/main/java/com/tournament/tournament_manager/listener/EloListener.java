package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.service.EloService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


import static com.tournament.tournament_manager.config.kafka.KafkaConfig.MATCH_FINISHED_TOPIC;

@Component
public class EloListener {

    private final EloService eloService;
    private final MatchRepository matchRepository;

    public EloListener(EloService eloService, MatchRepository matchRepository) {
        this.eloService = eloService;
        this.matchRepository = matchRepository;
    }

    @KafkaListener(topics = MATCH_FINISHED_TOPIC, groupId = "elo-group")
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = matchRepository.findById(event.matchId())
                .orElseThrow(() -> new MatchNotFoundException(event.matchId()));
        eloService.updateElo(match);
    }
}