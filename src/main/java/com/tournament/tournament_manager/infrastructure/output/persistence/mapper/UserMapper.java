package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.User;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        User user = new User();
        user.setId(entity.getId());
        user.setUsername(entity.getUsername());
        user.setPassword(entity.getPassword());
        user.setRole(entity.getRole());
        user.setCreatedAt(entity.getCreatedAt());
        return user;
    }

    public UserEntity toNewEntity(User user) {
        UserEntity entity = new UserEntity();
        updateEntity(entity, user);
        return entity;
    }

    public void updateEntity(UserEntity entity, User user) {
        entity.setUsername(user.getUsername());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole());
    }
}
