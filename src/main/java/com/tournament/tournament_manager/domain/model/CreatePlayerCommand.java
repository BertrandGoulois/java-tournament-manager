package com.tournament.tournament_manager.domain.model;

/**
 * Commande : créer un joueur. Aucune annotation de validation ni de documentation HTTP ici
 * — la validation (format du username, de l'email...) a lieu à la frontière REST/JSON-RPC,
 * sur le DTO de requête spécifique à chaque transport, avant construction de cette commande.
 */
public record CreatePlayerCommand(String username, String email) {}
