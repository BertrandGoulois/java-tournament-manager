package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.RefreshToken;
import com.tournament.tournament_manager.domain.port.out.auth.DeleteRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.LoadRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.SaveRefreshTokenPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RefreshTokenEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.RefreshTokenMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RefreshTokenRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter JPA implémentant les ports de gestion des refresh tokens.
 */
@Component
public class RefreshTokenJpaAdapter implements SaveRefreshTokenPort, LoadRefreshTokenPort, DeleteRefreshTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenMapper refreshTokenMapper;

    public RefreshTokenJpaAdapter(RefreshTokenRepository refreshTokenRepository,
                                  RefreshTokenMapper refreshTokenMapper) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @Override
    public RefreshToken saveRefreshToken(RefreshToken refreshToken) {
        RefreshTokenEntity entity;
        if (refreshToken.getId() != null) {
            entity = refreshTokenRepository.findById(refreshToken.getId())
                    .orElseGet(() -> refreshTokenMapper.toNewEntity(refreshToken));
            refreshTokenMapper.updateEntity(entity, refreshToken);
        } else {
            entity = refreshTokenMapper.toNewEntity(refreshToken);
        }
        RefreshTokenEntity saved = refreshTokenRepository.save(entity);
        return refreshTokenMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> loadByToken(String token) {
        return refreshTokenRepository.findByToken(token).map(refreshTokenMapper::toDomain);
    }

    @Override
    public void deleteByUsername(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }
}
