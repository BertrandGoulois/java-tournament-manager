package com.tournament.tournament_manager.service.bracket;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.tournament.StartTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.exception.InvalidException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cas d'utilisation : démarrage d'un tournoi et génération du bracket initial.
 */
@Service
@Transactional
public class StartTournamentService implements StartTournamentUseCase {

    private final LoadTournamentPort loadTournamentPort;
    private final SaveTournamentPort saveTournamentPort;
    private final LoadRegistrationPort loadRegistrationPort;
    private final SaveMatchPort saveMatchPort;

    public StartTournamentService(LoadTournamentPort loadTournamentPort,
                                  SaveTournamentPort saveTournamentPort,
                                  LoadRegistrationPort loadRegistrationPort,
                                  SaveMatchPort saveMatchPort) {
        this.loadTournamentPort = loadTournamentPort;
        this.saveTournamentPort = saveTournamentPort;
        this.loadRegistrationPort = loadRegistrationPort;
        this.saveMatchPort = saveMatchPort;
    }

    @Override
    public void startTournament(Long tournamentId) {
        Tournament tournament = loadTournamentPort.loadTournament(tournamentId);
        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new InvalidException("Tournament is not open");
        }
        List<Registration> registrations = loadRegistrationPort.loadByTournamentId(tournamentId);
        if (registrations.size() < 2) {
            throw new InvalidException("Tournament needs at least 2 players");
        }
        List<Player> players = registrations.stream()
                .map(Registration::getPlayer)
                .collect(Collectors.toList());
        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i += 2) {
            Player player1 = players.get(i);
            Player player2 = (i + 1 < players.size()) ? players.get(i + 1) : null;
            BracketUtils.createMatch(tournament, player1, player2,
                    BracketUtils.calculateFirstRound(players.size()), saveMatchPort);
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        saveTournamentPort.saveTournament(tournament);
    }
}