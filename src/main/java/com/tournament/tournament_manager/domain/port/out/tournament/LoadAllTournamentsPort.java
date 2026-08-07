package com.tournament.tournament_manager.domain.port.out.tournament;

import com.tournament.tournament_manager.domain.model.Tournament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port sortant : chargement de tous les tournois depuis la persistance.
 */
public interface LoadAllTournamentsPort {

    /**
     * Charge tous les tournois existants.
     *
     * @param pageable paramètres de pagination (page, taille, tri)
     * @return page de tournois, vide si aucun tournoi enregistré
     */
    Page<Tournament> loadAllTournaments(Pageable pageable);
}