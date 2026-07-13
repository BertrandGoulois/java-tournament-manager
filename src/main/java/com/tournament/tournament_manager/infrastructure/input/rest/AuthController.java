package com.tournament.tournament_manager.infrastructure.input.rest;

import com.tournament.tournament_manager.application.auth.AuthService;
import com.tournament.tournament_manager.domain.port.in.auth.RefreshTokenUseCase;
import com.tournament.tournament_manager.dto.request.auth.LoginRequest;
import com.tournament.tournament_manager.dto.request.auth.RefreshTokenRequest;
import com.tournament.tournament_manager.dto.response.auth.AuthResponse;
import com.tournament.tournament_manager.exception.handler.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Authentification JWT (login, refresh token, logout)")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(AuthService authService,
                          RefreshTokenUseCase refreshTokenUseCase) {
        this.authService = authService;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @Operation(summary = "Se connecter",
            description = "Authentifie un utilisateur et retourne un access token JWT et un refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie"),
            @ApiResponse(responseCode = "400", description = "Credentials invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Rafraîchir le token",
            description = "Génère un nouvel access token JWT à partir d'un refresh token valide.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token rafraîchi"),
            @ApiResponse(responseCode = "400", description = "Refresh token invalide ou révoqué",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenUseCase.refresh(request.refreshToken()));
    }

    @Operation(summary = "Se déconnecter",
            description = "Révoque le refresh token. Les appels ultérieurs avec ce token retourneront une erreur 400.")
    @ApiResponse(responseCode = "200", description = "Déconnexion réussie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenUseCase.revoke(request.refreshToken());
        return ResponseEntity.ok().build();
    }
}