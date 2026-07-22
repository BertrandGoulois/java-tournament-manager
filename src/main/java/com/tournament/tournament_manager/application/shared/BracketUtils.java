package com.tournament.tournament_manager.application.shared;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;

import java.time.LocalDateTime;

/**
 * Utilitaires partagés pour la gestion du bracket.
 */
public class BracketUtils {

    private BracketUtils() {}

    public static void createMatch(Tournament tournament, Player player1, Player player2,
                                   int round, SaveMatchPort saveMatchPort) {
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

    public static int calculateFirstRound(int playerCount) {
        if (playerCount <= 0) {
            throw new IllegalArgumentException("playerCount must be positive, got: " + playerCount);
        }
        int round = 1;
        while (round < playerCount) {
            round *= 2;
        }
        return round;
    }
}