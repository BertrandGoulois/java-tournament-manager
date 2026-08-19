package com.tournament.tournament_manager.domain.model;

import java.util.List;

/**
 * Classement complet d'un tournoi round-robin, trié par points décroissants. Vue agrégée,
 * pas une entité — construite par {@code GetStandingsService}.
 */
public record Standings(Long tournamentId, String tournamentName, List<StandingEntry> standings) {}
