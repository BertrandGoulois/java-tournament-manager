package com.tournament.tournament_manager.domain.port.in.auth;

import com.tournament.tournament_manager.domain.model.AuthResult;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Port entrant : cas d'utilisation pour l'authentification par mot de passe.
 */
public interface LoginUseCase {

    /**
     * Authentifie un utilisateur et retourne un access token JWT et un refresh token.
     *
     * @param username le nom d'utilisateur
     * @param password le mot de passe en clair
     * @return un access token JWT valide et un refresh token
     * @throws BadCredentialsException si le username ou le password est incorrect
     */
    AuthResult login(String username, String password);
}
