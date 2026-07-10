package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;

/**
 * Port entrant : cas d'utilisation pour supprimer (soft delete) un tournoi.
 */
public interface DeleteTournamentUseCase {

    /**
     * Désactive un tournoi sans le supprimer physiquement de la base.
     *
     * @param id identifiant du tournoi
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     */
    void deleteTournament(Long id);
}