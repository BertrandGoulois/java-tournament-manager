package com.tournament.tournament_manager.domain.port.out.match;

import com.tournament.tournament_manager.domain.model.entities.Match;

/**
 * Port sortant : sauvegarde d'un match en persistance.
 */
public interface SaveMatchPort {

    /**
     * Persiste un match et retourne l'entité sauvegardée.
     *
     * @param match le match à sauvegarder
     * @return le match sauvegardé
     */
    Match saveMatch(Match match);
}