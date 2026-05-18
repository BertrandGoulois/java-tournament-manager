package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Player;

import java.util.List;

/**
 * Port sortant : sauvegarde de plusieurs joueurs en persistance.
 */
public interface SaveAllPlayersPort {

    /**
     * Persiste une liste de joueurs.
     *
     * @param players la liste des joueurs à sauvegarder
     */
    void saveAllPlayers(List<Player> players);
}