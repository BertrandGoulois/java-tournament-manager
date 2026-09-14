package com.tournament.tournament_manager.domain.port.out.player;

import java.util.List;

import com.tournament.tournament_manager.domain.model.EloHistory;

/**
 * Port sortant : chargement de l'historique ELO d'un joueur.
 */
public interface LoadEloHistoryPort {

    /**
     * Charge l'historique ELO d'un joueur trié du plus récent au plus ancien.
     *
     * @param playerId identifiant du joueur
     * @return liste des entrées ELO triées par date décroissante
     */
    List<EloHistory> loadByPlayerIdOrderByDateDesc(Long playerId);
}
