package com.tournament.tournament_manager.domain.model;

/**
 * Commande : inscrire un joueur à un tournoi.
 */
public record RegisterPlayerCommand(Long playerId, Long tournamentId) {}
