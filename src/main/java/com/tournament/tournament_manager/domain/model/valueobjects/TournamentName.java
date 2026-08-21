package com.tournament.tournament_manager.domain.model.valueobjects;

import com.tournament.tournament_manager.exception.domain.InvalidException;

/**
 * Value object représentant le nom d'un tournoi.
 *
 * <p>Immuable, valide à la construction : un {@code TournamentName} ne peut jamais
 * représenter un nom vide ou blanc. Généralise le pattern déjà établi par
 * {@link EloRating} - voir sa Javadoc pour le principe général.
 */
public record TournamentName(String value) {

    public TournamentName {
        if (value == null || value.isBlank()) {
            throw new InvalidException("Le nom du tournoi ne peut pas être vide");
        }
    }
}
