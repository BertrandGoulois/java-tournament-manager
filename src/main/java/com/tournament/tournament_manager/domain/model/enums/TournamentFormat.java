package com.tournament.tournament_manager.domain.model.enums;

/**
 * Format de compétition d'un tournoi.
 */
public enum TournamentFormat {

    /** Élimination directe classique avec bracket. */
    SINGLE_ELIMINATION,

    /** Chaque joueur affronte tous les autres une fois, classement par points. */
    ROUND_ROBIN,

    /** Phase de groupes en round-robin, puis bracket en élimination directe entre qualifiés. */
    GROUPS_THEN_KNOCKOUT
}