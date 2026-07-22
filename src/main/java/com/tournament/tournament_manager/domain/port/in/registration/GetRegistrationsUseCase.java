package com.tournament.tournament_manager.domain.port.in.registration;

import com.tournament.tournament_manager.dto.response.registration.RegistrationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port entrant : cas d'utilisation pour consulter les inscriptions d'un tournoi.
 */
public interface GetRegistrationsUseCase {

    /**
     * Retourne la liste paginée des inscriptions d'un tournoi.
     *
     * @param tournamentId identifiant du tournoi
     * @param pageable     paramètres de pagination
     * @return page des inscriptions
     */
    Page<RegistrationResponse> getTournamentRegistrations(Long tournamentId, Pageable pageable);
}