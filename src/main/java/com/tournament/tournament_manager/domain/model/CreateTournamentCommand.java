package com.tournament.tournament_manager.domain.model;

import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;

/**
 * Commande : créer un tournoi. Aucune annotation de validation ni de documentation HTTP —
 * voir {@code CreatePlayerCommand}.
 */
public record CreateTournamentCommand(
        String name,
        int maxPlayers,
        TournamentFormat format,
        Integer numberOfGroups,
        Integer qualifiersPerGroup
) {}
