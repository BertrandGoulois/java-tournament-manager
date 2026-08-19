package com.tournament.tournament_manager.domain.model;

/**
 * Ligne de classement d'un joueur dans un tournoi round-robin.
 */
public record StandingEntry(Player player, int matchesPlayed, int wins, int losses, int points) {}
