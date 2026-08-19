package com.tournament.tournament_manager.domain.model;

/**
 * Commentaire narratif d'un match, généré par LLM ou en attente de génération
 * (voir {@code CommentaryListener}).
 */
public record MatchCommentary(Long matchId, String commentary) {}
