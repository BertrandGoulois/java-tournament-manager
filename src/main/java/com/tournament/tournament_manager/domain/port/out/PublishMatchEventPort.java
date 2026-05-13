package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;

public interface PublishMatchEventPort {
    void publishMatchFinished(MatchFinishedEvent event);
}