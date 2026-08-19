package com.tournament.tournament_manager.domain.model;

import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;

import java.util.List;

/**
 * Bracket complet d'un tournoi, organisé par round. Vue agrégée, pas une entité —
 * construite par {@code BracketQueryService} à partir de {@link Tournament} et des
 * {@link Match} groupés par round. Réutilise directement l'objet domaine {@link Match}
 * pour chaque match du bracket (mêmes champs qu'un {@code BracketMatchResponse} aurait
 * dupliqués : id, position, joueurs, vainqueur, statut — pas besoin d'un type séparé).
 */
public record Bracket(
        Long tournamentId,
        String tournamentName,
        TournamentStatus status,
        List<BracketRound> rounds
) {}
