package com.tournament.tournament_manager.domain.event;

import com.tournament.tournament_manager.domain.model.entities.Match;

public record MatchFinishedEvent(Match match) {
}
