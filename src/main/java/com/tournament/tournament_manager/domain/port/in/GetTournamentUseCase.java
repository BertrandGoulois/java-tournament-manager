package com.tournament.tournament_manager.domain.port.in;

import com.tournament.tournament_manager.dto.response.TournamentResponse;

import java.util.List;

/**
 * Port entrant : cas d'utilisation pour consulter un ou plusieurs tournois.
 */
public interface GetTournamentUseCase {

    /**
     * Retourne un tournoi par son identifiant.
     *
     * @param id identifiant du tournoi
     * @return la représentation du tournoi
     * @throws com.tournament.tournament_manager.exception.TournamentNotFoundException si le tournoi n'existe pas
     */
    TournamentResponse getTournamentById(Long id);

    /**
     * Retourne la liste de tous les tournois.
     *
     * @return liste des tournois, vide si aucun tournoi enregistré
     */
    List<TournamentResponse> getAllTournaments();
}
