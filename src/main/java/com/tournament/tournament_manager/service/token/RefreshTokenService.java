package com.tournament.tournament_manager.service.token;

import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.domain.model.entities.RefreshToken;
import com.tournament.tournament_manager.domain.port.in.auth.RefreshTokenUseCase;
import com.tournament.tournament_manager.domain.port.out.auth.DeleteRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.LoadRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.SaveRefreshTokenPort;
import com.tournament.tournament_manager.dto.response.auth.AuthResponse;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implémentation des cas d'utilisation liés au refresh token JWT.
 *
 * <p>Le refresh token est un UUID aléatoire stocké en base avec une date d'expiration.
 * Il permet d'obtenir un nouveau access token JWT sans ressaisir ses credentials.
 */
@Service
@Transactional(readOnly = true)
public class RefreshTokenService implements RefreshTokenUseCase {

    private final JwtService jwtService;
    private final SaveRefreshTokenPort saveRefreshTokenPort;
    private final LoadRefreshTokenPort loadRefreshTokenPort;
    private final DeleteRefreshTokenPort deleteRefreshTokenPort;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public RefreshTokenService(JwtService jwtService,
                               SaveRefreshTokenPort saveRefreshTokenPort,
                               LoadRefreshTokenPort loadRefreshTokenPort,
                               DeleteRefreshTokenPort deleteRefreshTokenPort) {
        this.jwtService = jwtService;
        this.saveRefreshTokenPort = saveRefreshTokenPort;
        this.loadRefreshTokenPort = loadRefreshTokenPort;
        this.deleteRefreshTokenPort = deleteRefreshTokenPort;
    }

    /**
     * Génère et persiste un nouveau refresh token pour un utilisateur.
     *
     * @param username le username de l'utilisateur
     * @return le refresh token généré
     */
    @Transactional
    public String generateRefreshToken(String username) {
        deleteRefreshTokenPort.deleteByUsername(username);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUsername(username);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshExpiration / 1000));

        return saveRefreshTokenPort.saveRefreshToken(refreshToken).getToken();
    }

    /**
     * Génère un nouveau access token à partir d'un refresh token valide.
     *
     * @param refreshToken le refresh token
     * @return un nouvel access token JWT
     * @throws InvalidException si le token est invalide, expiré ou révoqué
     */
    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken token = loadRefreshTokenPort.loadByToken(refreshToken)
                .orElseThrow(() -> new InvalidException("Refresh token invalide"));

        if (token.isRevoked()) {
            throw new InvalidException("Refresh token révoqué");
        }
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidException("Refresh token expiré");
        }

        String newAccessToken = jwtService.generateToken(token.getUsername());
        return new AuthResponse(newAccessToken, refreshToken);
    }

    /**
     * Révoque le refresh token (logout).
     *
     * @param refreshToken le refresh token à révoquer
     * @throws InvalidException si le token n'existe pas
     */
    @Override
    @Transactional
    public void revoke(String refreshToken) {
        RefreshToken token = loadRefreshTokenPort.loadByToken(refreshToken)
                .orElseThrow(() -> new InvalidException("Refresh token invalide"));
        token.setRevoked(true);
        saveRefreshTokenPort.saveRefreshToken(token);
    }
}