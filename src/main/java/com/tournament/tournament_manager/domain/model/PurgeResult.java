package com.tournament.tournament_manager.domain.model;

/**
 * Résultat d'une exécution de purge : le nombre d'entités traitées dans chaque catégorie.
 * Vue agrégée, pas une entité — construite par {@code PurgeService}.
 */
public record PurgeResult(
        int anonymizedPlayers,
        int purgedPlayers,
        int purgedTournaments,
        int purgedRefreshTokens,
        int purgedOutboxEvents
) {}
