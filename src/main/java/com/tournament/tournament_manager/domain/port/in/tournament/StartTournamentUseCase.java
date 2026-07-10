package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;

/**
 * Port entrant : cas d'utilisation pour démarrer un tournoi.
 */
public interface StartTournamentUseCase {

    /**
     * Démarre un tournoi et génère le bracket du premier tour.
     *
     * @param tournamentId identifiant du tournoi à démarrer
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     * @throws InvalidException si le tournoi n'est pas au statut {@code OPEN}
     * @throws InvalidException si moins de 2 joueurs sont inscrits
     */
    void startTournament(Long tournamentId);
}
