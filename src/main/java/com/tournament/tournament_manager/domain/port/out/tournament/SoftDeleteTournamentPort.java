package com.tournament.tournament_manager.domain.port.out.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;

/**
 * Port sortant : soft delete d'un tournoi en persistance.
 */
public interface SoftDeleteTournamentPort {

    /**
     * Marque un tournoi comme supprimé et persiste la modification.
     *
     * @param tournament le tournoi à désactiver
     */
    void softDeleteTournament(Tournament tournament);
}