package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Tournament;

/**
 * Port sortant : sauvegarde d'un tournoi en persistance.
 */
public interface SaveTournamentPort {

    /**
     * Persiste un tournoi et retourne l'entité sauvegardée.
     *
     * @param tournament le tournoi à sauvegarder
     * @return le tournoi sauvegardé
     */
    Tournament saveTournament(Tournament tournament);
}

