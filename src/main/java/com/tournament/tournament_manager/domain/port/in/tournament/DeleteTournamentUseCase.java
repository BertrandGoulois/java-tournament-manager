package com.tournament.tournament_manager.domain.port.in.tournament;

/**
 * Port entrant : cas d'utilisation pour supprimer (soft delete) un tournoi.
 */
public interface DeleteTournamentUseCase {

    /**
     * Désactive un tournoi sans le supprimer physiquement de la base.
     *
     * @param id identifiant du tournoi
     * @throws com.tournament.tournament_manager.exception.TournamentNotFoundException si le tournoi n'existe pas
     */
    void deleteTournament(Long id);
}