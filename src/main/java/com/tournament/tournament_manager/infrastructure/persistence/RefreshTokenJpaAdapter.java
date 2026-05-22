package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.RefreshToken;
import com.tournament.tournament_manager.domain.port.out.auth.DeleteRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.LoadRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.SaveRefreshTokenPort;
import com.tournament.tournament_manager.repository.RefreshTokenRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter JPA implémentant les ports de gestion des refresh tokens.
 */
@Component
public class RefreshTokenJpaAdapter implements SaveRefreshTokenPort, LoadRefreshTokenPort, DeleteRefreshTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenJpaAdapter(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RefreshToken saveRefreshToken(RefreshToken refreshToken) {
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> loadByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public void deleteByUsername(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }
}