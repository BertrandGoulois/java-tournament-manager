package com.tournament.tournament_manager.domain.port.out;

/**
 * Port sortant : vérification d'existence d'un tournoi par son nom.
 */
public interface ExistsTournamentPort {

    /**
     * Vérifie si un tournoi existe avec ce nom.
     *
     * @param name le nom à vérifier
     * @return {@code true} si un tournoi avec ce nom existe déjà
     */
    boolean existsByName(String name);
}
