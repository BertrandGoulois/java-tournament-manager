package com.tournament.tournament_manager.domain.port.in.registration;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Registration;

/**
 * Port entrant : cas d'utilisation pour consulter les inscriptions d'un tournoi.
 */
public interface GetRegistrationsUseCase {

    /**
     * Retourne la liste paginée des inscriptions d'un tournoi.
     *
     * @param tournamentId identifiant du tournoi
     * @param pageRequest  paramètres de pagination
     * @return page des inscriptions
     */
    PageResult<Registration> getTournamentRegistrations(Long tournamentId, PageRequest pageRequest);
}
