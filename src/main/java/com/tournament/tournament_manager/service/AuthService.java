package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.dto.request.LoginRequest;
import com.tournament.tournament_manager.dto.response.AuthResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Vérifie qu'un entier est une puissance de 2.
 * Utilise l'astuce bit-à-bit : {@code n > 0 && (n & (n - 1)) == 0}.
 *
 * @param n la valeur à tester
 * @return {@code true} si {@code n} est une puissance de 2
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Authentifie un utilisateur et retourne un token JWT.
     *
     * <p>La vérification des credentials est déléguée à l'{@code AuthenticationManager}
     * de Spring Security. En cas d'échec, Spring lève une
     * {@code BadCredentialsException} avant même d'atteindre la génération du token.
     *
     * @param request contient le username et le password
     * @return un token JWT valide
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         si le username ou le password est incorrect
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        String token = jwtService.generateToken(request.username());
        return new AuthResponse(token);
    }
}
