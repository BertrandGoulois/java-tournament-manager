package com.tournament.tournament_manager.service.tournament;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.tournament.StartTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.exception.InvalidException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cas d'utilisation : démarrage d'un tournoi.
 *
 * <p>Délègue la génération des matchs initiaux à la {@link TournamentStartStrategy}
 * correspondant au format du tournoi (élimination directe, round-robin...).
 */
@Service
@Transactional
public class StartTournamentService implements StartTournamentUseCase {

    private final LoadTournamentPort loadTournamentPort;
    private final SaveTournamentPort saveTournamentPort;
    private final LoadRegistrationPort loadRegistrationPort;
    private final Map<com.tournament.tournament_manager.domain.model.enums.TournamentFormat, TournamentStartStrategy> strategies;

    public StartTournamentService(LoadTournamentPort loadTournamentPort,
                                  SaveTournamentPort saveTournamentPort,
                                  LoadRegistrationPort loadRegistrationPort,
                                  List<TournamentStartStrategy> strategyList) {
        this.loadTournamentPort = loadTournamentPort;
        this.saveTournamentPort = saveTournamentPort;
        this.loadRegistrationPort = loadRegistrationPort;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(TournamentStartStrategy::supportedFormat, s -> s));
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

        TournamentStartStrategy strategy = strategies.get(tournament.getFormat());
        if (strategy == null) {
            throw new InvalidException("No start strategy registered for format " + tournament.getFormat());
        }
        strategy.generateInitialMatches(tournament, players);

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        saveTournamentPort.saveTournament(tournament);
    }
}