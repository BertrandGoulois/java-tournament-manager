package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.dto.request.LoginRequest;
import com.tournament.tournament_manager.dto.response.AuthResponse;
import com.tournament.tournament_manager.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée HTTP pour l'authentification.
 * Endpoint public, non protégé par le filtre JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authentifie un utilisateur et retourne un token JWT.
     *
     * @param request contient le username et le password
     * @return {@code 200 OK} avec le token JWT
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         si les credentials sont incorrects ({@code 403})
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
