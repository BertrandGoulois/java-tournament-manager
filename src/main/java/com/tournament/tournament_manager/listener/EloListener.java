package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.service.EloService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EloListener {

    private final EloService eloService;

    public EloListener(EloService eloService) {
        this.eloService = eloService;
    }

    @EventListener
    public void onMatchFinished(MatchFinishedEvent event) {
        eloService.updateElo(event.match());
    }
}