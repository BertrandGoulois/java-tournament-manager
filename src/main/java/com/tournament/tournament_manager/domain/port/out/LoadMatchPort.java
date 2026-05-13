package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Match;

public interface LoadMatchPort {
    Match loadMatch(Long id);
}