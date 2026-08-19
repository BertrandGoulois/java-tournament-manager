package com.tournament.tournament_manager.application.auth;

import com.tournament.tournament_manager.application.token.RefreshTokenService;
import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.domain.model.AuthResult;
import com.tournament.tournament_manager.domain.port.in.auth.LoginUseCase;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation : authentification par mot de passe. Retourne un objet de domaine pur
 * ({@link AuthResult}) — voir la Javadoc de {@code GetPlayerService}.
 */
@Service
public class AuthService implements LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Authentifie un utilisateur et retourne un token JWT.
     *
     * <p>La vérification des credentials est déléguée à l'{@code AuthenticationManager}
     * de Spring Security. En cas d'échec, Spring lève une
     * {@code BadCredentialsException} avant même d'atteindre la génération du token.
     *
     * @param username le nom d'utilisateur
     * @param password le mot de passe en clair
     * @return un access token JWT valide et un refresh token
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         si le username ou le password est incorrect
     */
    @Override
    public AuthResult login(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        String accessToken = jwtService.generateToken(username);
        String refreshToken = refreshTokenService.generateRefreshToken(username);
        return new AuthResult(accessToken, refreshToken);
    }
}
