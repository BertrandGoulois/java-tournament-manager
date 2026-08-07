package com.tournament.tournament_manager.domain.port.out.tournament;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Tournament;

/**
 * Port sortant : chargement de tous les tournois depuis la persistance.
 */
public interface LoadAllTournamentsPort {

    /**
     * Charge tous les tournois existants.
     *
     * @param pageRequest paramètres de pagination (page, taille)
     * @return page de tournois, vide si aucun tournoi enregistré
     */
    PageResult<Tournament> loadAllTournaments(PageRequest pageRequest);
}
