package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Retourne une page de tous les tournois.
     *
     * @return page de tournois, vide si aucun tournoi enregistré
     */
    Page<TournamentResponse> getAllTournaments(Pageable pageable);
}
