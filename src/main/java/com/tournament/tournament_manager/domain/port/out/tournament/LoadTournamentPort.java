package com.tournament.tournament_manager.domain.port.out.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;

/**
 * Port sortant : chargement d'un tournoi depuis la persistance.
 */
public interface LoadTournamentPort {

    /**
     * Charge un tournoi par son identifiant.
     *
     * @param id identifiant du tournoi
     * @return le tournoi correspondant
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     */
    Tournament loadTournament(Long id);
}
