package com.tournament.tournament_manager.domain.port.out.registration;

import com.tournament.tournament_manager.domain.model.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * @param pageable     paramètres de pagination
     * @return page des inscriptions
     */
    Page<Registration> loadByTournamentId(Long tournamentId, Pageable pageable);
}