package com.tournament.tournament_manager.infrastructure.output.security;

import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.domain.port.out.auth.TokenProviderPort;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant reliant {@link TokenProviderPort} à l'implémentation JWT concrète.
 *
 * <p>Volontairement minuscule : c'est le propre d'un adaptateur à cet endroit. Sa seule
 * raison d'être est que la couche applicative cesse de nommer {@code JwtService}, et donc
 * cesse de dépendre d'un choix technologique qui ne la regarde pas. Toute logique ajoutée
 * ici serait au mauvais endroit — le métier va dans {@code application}, la cryptographie
 * dans {@code JwtService}.
 */
@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final JwtService jwtService;

    public JwtTokenProviderAdapter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String generateAccessToken(String username) {
        return jwtService.generateToken(username);
    }
}
