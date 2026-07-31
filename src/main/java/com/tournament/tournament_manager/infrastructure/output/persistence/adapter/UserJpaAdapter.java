package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.port.out.auth.UserExistsPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter JPA implémentant les ports de domaine relatifs aux utilisateurs.
 */
@Component
public class UserJpaAdapter implements UserExistsPort {

    private final UserRepository userRepository;

    public UserJpaAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
