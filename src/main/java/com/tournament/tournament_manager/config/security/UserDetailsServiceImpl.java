package com.tournament.tournament_manager.config.security;

import com.tournament.tournament_manager.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implémentation de {@code UserDetailsService} qui charge les utilisateurs
 * depuis la table {@code users} via {@code UserRepository}.
 *
 * <p>Le rôle est préfixé par {@code ROLE_} pour respecter la convention Spring Security
 * (ex. {@code Role.ADMIN} → {@code "ROLE_ADMIN"}).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charge un utilisateur par son username.
     *
     * @param username le username recherché
     * @return les détails de l'utilisateur
     * @throws UsernameNotFoundException si aucun utilisateur ne correspond
     */
    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> new User(
                        user.getUsername(),
                        user.getPassword(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}