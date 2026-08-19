package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;

/**
 * Port entrant : cas d'utilisation pour consulter un ou plusieurs tournois.
 */
public interface GetTournamentUseCase {

    /**
     * Retourne un tournoi par son identifiant.
     *
     * @param id identifiant du tournoi
     * @return le tournoi
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     */
    Tournament getTournamentById(Long id);

    /**
     * Retourne une page de tous les tournois.
     *
     * @return page de tournois, vide si aucun tournoi enregistré
     */
    PageResult<Tournament> getAllTournaments(PageRequest pageRequest);
}
