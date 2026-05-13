package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Tournament;

/**
 * Port sortant : chargement d'un tournoi depuis la persistance.
 */
public interface LoadTournamentPort {

    /**
     * Charge un tournoi par son identifiant.
     *
     * @param id identifiant du tournoi
     * @return le tournoi correspondant
     * @throws com.tournament.tournament_manager.exception.TournamentNotFoundException si le tournoi n'existe pas
     */
    Tournament loadTournament(Long id);
}
