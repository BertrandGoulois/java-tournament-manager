package com.tournament.tournament_manager.domain.model;

/**
 * Résultat d'une authentification ou d'un rafraîchissement de token : un access token JWT
 * et un refresh token.
 */
public record AuthResult(String accessToken, String refreshToken) {}
