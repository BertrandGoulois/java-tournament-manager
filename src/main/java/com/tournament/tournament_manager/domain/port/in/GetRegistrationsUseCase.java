package com.tournament.tournament_manager.domain.port.in;

import com.tournament.tournament_manager.dto.response.RegistrationResponse;

import java.util.List;

/**
 * Port entrant : cas d'utilisation pour consulter les inscriptions d'un tournoi.
 */
public interface GetRegistrationsUseCase {

    /**
     * Retourne la liste des inscriptions d'un tournoi.
     *
     * @param tournamentId identifiant du tournoi
     * @return liste des inscriptions, vide si aucun joueur inscrit
     */
    List<RegistrationResponse> getTournamentRegistrations(Long tournamentId);
}
