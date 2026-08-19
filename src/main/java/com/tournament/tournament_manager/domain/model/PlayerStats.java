package com.tournament.tournament_manager.domain.model;

import java.util.List;

/**
 * Statistiques complètes d'un joueur : matchs joués, victoires, défaites, win rate et
 * historique ELO. Vue agrégée, pas une entité — construite par
 * {@code GetPlayerStatsService} à partir de {@link Player} et {@link EloHistory}.
 */
public record PlayerStats(
        Player player,
        int matchesPlayed,
        int wins,
        int losses,
        double winRate,
        List<EloHistory> eloHistory
) {}
