package com.tournament.tournament_manager.domain.port.in;

import com.tournament.tournament_manager.domain.model.entities.Match;

/**
 * Port entrant : cas d'utilisation pour mettre à jour les classements ELO après un match.
 */
public interface UpdateEloUseCase {

    /**
     * Met à jour le classement ELO des deux joueurs d'un match terminé.
     *
     * @param match le match terminé, avec {@code winner} renseigné et {@code player2} non null
     */
    void updateElo(Match match);
}