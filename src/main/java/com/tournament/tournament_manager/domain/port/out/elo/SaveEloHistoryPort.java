package com.tournament.tournament_manager.domain.port.out.elo;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;

/**
 * Port sortant : sauvegarde d'une entrée d'historique ELO.
 */
public interface SaveEloHistoryPort {

    /**
     * Persiste une entrée d'historique ELO.
     *
     * @param eloHistory l'entrée à sauvegarder
     */
    void saveEloHistory(EloHistory eloHistory);
}