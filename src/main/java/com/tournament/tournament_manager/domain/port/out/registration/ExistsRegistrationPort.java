package com.tournament.tournament_manager.domain.port.out.registration;

/**
 * Port sortant : vérification d'existence d'une inscription.
 */
public interface ExistsRegistrationPort {

    /**
     * Vérifie si un joueur est déjà inscrit à un tournoi.
     *
     * @param playerId     identifiant du joueur
     * @param tournamentId identifiant du tournoi
     * @return {@code true} si le joueur est déjà inscrit
     */
    boolean existsByPlayerIdAndTournamentId(Long playerId, Long tournamentId);
}
