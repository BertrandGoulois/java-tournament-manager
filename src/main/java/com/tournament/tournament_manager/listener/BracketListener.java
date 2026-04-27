package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.service.BracketService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BracketListener {

    private final BracketService bracketService;

    public BracketListener(BracketService bracketService){
        this.bracketService = bracketService;
    }

    @EventListener
    public void onMatchFinished(MatchFinishedEvent event) {
        bracketService.advanceToNextRound(event.match().getTournament(), event.match().getRound());
    }
}
