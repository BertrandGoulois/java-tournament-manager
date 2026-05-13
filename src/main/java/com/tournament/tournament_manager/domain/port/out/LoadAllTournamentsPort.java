package com.tournament.tournament_manager.domain.port.out;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import java.util.List;

/**
 * Port sortant : chargement de tous les tournois depuis la persistance.
 */
public interface LoadAllTournamentsPort {

    /**
     * Charge tous les tournois existants.
     *
     * @return liste des tournois, vide si aucun tournoi enregistré
     */
    List<Tournament> loadAllTournaments();
}