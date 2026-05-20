package com.tournament.tournament_manager.domain.port.out;

/**
 * Port sortant : vérification d'existence d'un historique ELO pour un match.
 */
public interface ExistsEloHistoryPort {

    /**
     * Vérifie si un historique ELO existe déjà pour ce match.
     * Utilisé pour garantir l'idempotence du calcul ELO.
     *
     * @param matchId identifiant du match
     * @return {@code true} si l'historique existe déjà
     */
    boolean existsByMatchId(Long matchId);
}