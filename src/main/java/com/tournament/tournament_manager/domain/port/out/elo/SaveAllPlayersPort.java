package com.tournament.tournament_manager.domain.port.out.elo;

import java.util.List;

import com.tournament.tournament_manager.domain.model.Player;

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
