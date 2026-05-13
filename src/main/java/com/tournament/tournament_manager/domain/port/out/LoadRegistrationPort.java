package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Registration;

import java.util.List;

/**
 * Port sortant : chargement de toutes les inscriptions d'un tournoi.
 */
public interface LoadRegistrationPort {

    /**
     * Charge toutes les inscriptions d'un tournoi.
     *
     * @param tournamentId identifiant du tournoi
     * @return liste des inscriptions, vide si aucun joueur inscrit
     */
    List<Registration> loadByTournamentId(Long tournamentId);
}
