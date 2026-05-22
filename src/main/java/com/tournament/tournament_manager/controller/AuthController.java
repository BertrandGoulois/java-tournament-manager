package com.tournament.tournament_manager.controller;

import com.tournament.tournament_manager.domain.port.in.auth.RefreshTokenUseCase;
import com.tournament.tournament_manager.dto.request.LoginRequest;
import com.tournament.tournament_manager.dto.request.RefreshTokenRequest;
import com.tournament.tournament_manager.dto.response.AuthResponse;
import com.tournament.tournament_manager.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée HTTP pour l'authentification.
 * Endpoint public, non protégé par le filtre JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(AuthService authService,
                          RefreshTokenUseCase refreshTokenUseCase) {
        this.authService = authService;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    /**
     * Authentifie un utilisateur et retourne un access token JWT et un refresh token.
     *
     * @param request contient le username et le password
     * @return {@code 200 OK} avec l'access token et le refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Génère un nouvel access token à partir d'un refresh token valide.
     *
     * @param request contient le refresh token
     * @return {@code 200 OK} avec un nouvel access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenUseCase.refresh(request.refreshToken()));
    }

    /**
     * Révoque le refresh token (logout).
     *
     * @param request contient le refresh token à révoquer
     * @return {@code 200 OK}
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenUseCase.revoke(request.refreshToken());
        return ResponseEntity.ok().build();
    }
}