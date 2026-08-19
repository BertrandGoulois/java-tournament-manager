package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.domain.model.CreateTournamentCommand;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.exception.domain.InvalidTournamentException;
import com.tournament.tournament_manager.exception.domain.TournamentAlreadyExistsException;

/**
 * Port entrant : cas d'utilisation pour créer un tournoi.
 */
public interface CreateTournamentUseCase {

    /**
     * Crée un nouveau tournoi au statut {@code OPEN}.
     *
     * @param command contient le nom, le nombre maximum de joueurs et le format
     * @return le tournoi créé
     * @throws TournamentAlreadyExistsException si le nom est déjà utilisé
     * @throws InvalidTournamentException si {@code maxPlayers} n'est pas une puissance de 2
     */
    Tournament createTournament(CreateTournamentCommand command);
}
