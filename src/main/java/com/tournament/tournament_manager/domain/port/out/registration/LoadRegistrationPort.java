package com.tournament.tournament_manager.domain.port.out.registration;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Registration;

import java.util.List;

/**
 * Port sortant : chargement des inscriptions d'un tournoi.
 */
public interface LoadRegistrationPort {

    /**
     * Charge toutes les inscriptions d'un tournoi (sans pagination).
     * Utilisé uniquement en interne (démarrage du tournoi).
     */
    List<Registration> loadByTournamentId(Long tournamentId);

    /**
     * Charge les inscriptions d'un tournoi de façon paginée.
     *
     * @param tournamentId identifiant du tournoi
     * @param pageRequest  paramètres de pagination
     * @return page des inscriptions
     */
    PageResult<Registration> loadByTournamentId(Long tournamentId, PageRequest pageRequest);
}
