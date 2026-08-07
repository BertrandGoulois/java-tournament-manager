package com.tournament.tournament_manager.domain.port.out.strategy;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;

/**
 * Stratégie de progression d'un tournoi après la fin d'un match,
 * propre à un format donné.
 */
public interface TournamentProgressionStrategy {

    /**
     * Indique le format de tournoi pris en charge par cette stratégie.
     *
     * @return le format supporté
     */
    TournamentFormat supportedFormat();

    /**
     * Fait progresser le tournoi après la fin du match donné.
     *
     * @param match      le match qui vient de se terminer
     * @param tournament le tournoi concerné
     */
    void onMatchFinished(Match match, Tournament tournament);
}