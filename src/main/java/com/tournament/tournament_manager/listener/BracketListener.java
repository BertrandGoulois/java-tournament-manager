package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.service.BracketService;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.tournament.tournament_manager.config.KafkaConfig.MATCH_FINISHED_TOPIC;

@Component
public class BracketListener {

    private final BracketService bracketService;
    private final MatchRepository matchRepository;

    public BracketListener(BracketService bracketService, MatchRepository matchRepository){
        this.bracketService = bracketService;
        this.matchRepository = matchRepository;
    }

    @KafkaListener(topics = MATCH_FINISHED_TOPIC, groupId = "bracket-group")
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = matchRepository.findById(event.matchId())
                .orElseThrow(() -> new MatchNotFoundException(event.matchId()));
        bracketService.advanceToNextRound(match.getTournament(), match.getRound());
    }
}
