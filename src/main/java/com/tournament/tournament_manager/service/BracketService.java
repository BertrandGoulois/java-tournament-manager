package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.in.StartTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.LoadMatchByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.SaveTournamentPort;
import com.tournament.tournament_manager.exception.InvalidException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gère la génération et la progression du bracket en élimination directe.
 *
 * <p>Le numéro de round suit une convention décroissante : la valeur de départ est
 * la première puissance de 2 supérieure ou égale au nombre de joueurs inscrits
 * (ex. 8 pour 6 joueurs), et chaque tour suivant divise ce numéro par 2.
 * Le tournoi se termine quand {@code nextRound < 2}.
 *
 * <p>Les joueurs sans adversaire (byes) reçoivent un match {@code FINISHED}
 * immédiatement, avec eux-mêmes déclarés vainqueurs, afin d'homogénéiser
 * le traitement dans {@link #advanceToNextRound}.
 *
 * <p>Dépend uniquement de ports (interfaces) - aucune dépendance directe vers JPA.
 */
@Service
@Transactional(readOnly = true)
public class BracketService implements StartTournamentUseCase, AdvanceBracketUseCase {

    private final LoadTournamentPort loadTournamentPort;
    private final SaveTournamentPort saveTournamentPort;
    private final LoadRegistrationPort loadRegistrationPort;
    private final SaveMatchPort saveMatchPort;
    private final LoadMatchByTournamentPort loadMatchByTournamentPort;

    public BracketService(LoadTournamentPort loadTournamentPort,
                          SaveTournamentPort saveTournamentPort,
                          LoadRegistrationPort loadRegistrationPort,
                          SaveMatchPort saveMatchPort,
                          LoadMatchByTournamentPort loadMatchByTournamentPort) {
        this.loadTournamentPort = loadTournamentPort;
        this.saveTournamentPort = saveTournamentPort;
        this.loadRegistrationPort = loadRegistrationPort;
        this.saveMatchPort = saveMatchPort;
        this.loadMatchByTournamentPort = loadMatchByTournamentPort;
    }

    @Override
    @Transactional
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
            createMatch(tournament, player1, player2, calculateFirstRound(players.size()));
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        saveTournamentPort.saveTournament(tournament);
    }

    @Transactional
    public void advanceToNextRound(Tournament tournament, int currentRound) {
        List<Match> currentMatches = loadMatchByTournamentPort
                .loadByTournamentIdAndRound(tournament.getId(), currentRound);
        boolean allFinished = currentMatches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
        if (!allFinished) return;

        int nextRound = currentRound / 2;
        if (nextRound < 2) {
            tournament.setStatus(TournamentStatus.FINISHED);
            saveTournamentPort.saveTournament(tournament);
            return;
        }
        List<Player> winners = currentMatches.stream()
                .map(Match::getWinner)
                .collect(Collectors.toList());
        Collections.shuffle(winners);
        for (int i = 0; i < winners.size(); i += 2) {
            Player player1 = winners.get(i);
            Player player2 = (i + 1 < winners.size()) ? winners.get(i + 1) : null;
            createMatch(tournament, player1, player2, nextRound);
        }
    }

    private void createMatch(Tournament tournament, Player player1, Player player2, int round) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(round);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        if (player2 == null) {
            match.setStatus(MatchStatus.FINISHED);
            match.setWinner(player1);
            match.setPlayedAt(LocalDateTime.now());
        } else {
            match.setStatus(MatchStatus.PENDING);
        }
        saveMatchPort.saveMatch(match);
    }

    private int calculateFirstRound(int playerCount) {
        int round = 1;
        while (round < playerCount) {
            round *= 2;
        }
        return round;
    }
}