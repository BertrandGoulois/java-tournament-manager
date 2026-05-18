package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Player;

import java.util.List;

/**
 * Port sortant : chargement de tous les joueurs depuis la persistance.
 */
public interface LoadAllPlayersPort {

    /**
     * Charge tous les joueurs existants.
     *
     * @return liste des joueurs, vide si aucun joueur enregistré
     */
    List<Player> loadAllPlayers();
}