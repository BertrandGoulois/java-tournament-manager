package com.tournament.tournament_manager.domain.port.in.registration;

import com.tournament.tournament_manager.domain.model.RegisterPlayerCommand;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;

/**
 * Port entrant : cas d'utilisation pour inscrire un joueur à un tournoi.
 */
public interface RegisterPlayerUseCase {

    /**
     * Inscrit un joueur à un tournoi.
     *
     * @param command contient l'identifiant du joueur et du tournoi
     * @return l'inscription créée
     * @throws PlayerNotFoundException     si le joueur n'existe pas
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     * @throws InvalidException            si le tournoi n'est pas ouvert
     * @throws InvalidException            si le joueur est déjà inscrit
     * @throws InvalidException            si le tournoi est complet
     */
    Registration registerPlayer(RegisterPlayerCommand command);
}
