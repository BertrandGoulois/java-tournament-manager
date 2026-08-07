package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.RefreshToken;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

    public RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        RefreshToken token = new RefreshToken();
        token.setId(entity.getId());
        token.setToken(entity.getToken());
        token.setUsername(entity.getUsername());
        token.setExpiryDate(entity.getExpiryDate());
        token.setRevoked(entity.isRevoked());
        token.setCreatedAt(entity.getCreatedAt());
        return token;
    }

    public RefreshTokenEntity toNewEntity(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        updateEntity(entity, token);
        return entity;
    }

    public void updateEntity(RefreshTokenEntity entity, RefreshToken token) {
        entity.setToken(token.getToken());
        entity.setUsername(token.getUsername());
        entity.setExpiryDate(token.getExpiryDate());
        entity.setRevoked(token.isRevoked());
    }
}
