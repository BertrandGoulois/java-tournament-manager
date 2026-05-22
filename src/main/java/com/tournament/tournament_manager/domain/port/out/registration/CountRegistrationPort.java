package com.tournament.tournament_manager.domain.port.out.registration;

/**
 * Port sortant : comptage des inscriptions d'un tournoi.
 */
public interface CountRegistrationPort {

    /**
     * Compte le nombre d'inscrits à un tournoi.
     *
     * @param tournamentId identifiant du tournoi
     * @return nombre d'inscriptions
     */
    long countByTournamentId(Long tournamentId);
}
