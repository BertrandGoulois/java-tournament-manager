package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;

/**
 * Port entrant : cas d'utilisation pour faire progresser le bracket au tour suivant.
 */
public interface AdvanceBracketUseCase {

    /**
     * Tente de faire progresser le bracket au tour suivant.
     * N'effectue aucune action si tous les matchs du round en cours ne sont pas terminés.
     *
     * @param tournament   le tournoi concerné
     * @param currentRound le numéro du round qui vient de se terminer
     */
    void advanceToNextRound(Tournament tournament, int currentRound);
}