package com.tournament.tournament_manager.domain.port.out.strategy;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;

import java.util.List;

/**
 * Stratégie de génération des matchs initiaux d'un tournoi, propre à un format donné.
 */
public interface TournamentStartStrategy {

    /**
     * Indique le format de tournoi pris en charge par cette stratégie.
     *
     * @return le format supporté
     */
    TournamentFormat supportedFormat();

    /**
     * Génère les matchs initiaux du tournoi selon le format.
     *
     * @param tournament le tournoi à démarrer
     * @param players    les joueurs inscrits
     */
    void generateInitialMatches(Tournament tournament, List<Player> players);
}