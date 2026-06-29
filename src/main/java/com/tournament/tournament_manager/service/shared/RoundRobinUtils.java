package com.tournament.tournament_manager.service.shared;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Génération de matchs round-robin via la méthode du cercle, réutilisable
 * à la fois pour un tournoi {@code ROUND_ROBIN} pur et pour chaque groupe
 * d'un tournoi {@code GROUPS_THEN_KNOCKOUT}.
 *
 * <p>Un joueur reste fixe, les autres tournent à chaque round, garantissant
 * que chaque paire de joueurs se rencontre exactement une fois. Si l'effectif
 * est impair, un joueur "fantôme" (bye) est ajouté : le joueur qui lui est
 * associé à un round donné ne joue pas ce round-là.
 */
public class RoundRobinUtils {

    private RoundRobinUtils() {}

    /**
     * Génère et persiste l'intégralité des confrontations round-robin pour un groupe de joueurs.
     *
     * @param tournament   le tournoi concerné
     * @param players      les joueurs participant à ce round-robin (un groupe, ou tous les joueurs
     *                      pour un tournoi ROUND_ROBIN pur)
     * @param groupNumber  numéro du groupe ({@code null} pour un round-robin pur, sans notion de groupe)
     * @param saveMatchPort port de persistance des matchs
     */
    public static void generateRoundRobinMatches(Tournament tournament, List<Player> players,
                                                 Integer groupNumber, SaveMatchPort saveMatchPort) {
        List<Player> rotation = new ArrayList<>(players);
        Collections.shuffle(rotation);

        boolean hasGhost = rotation.size() % 2 != 0;
        if (hasGhost) {
            rotation.add(null);
        }

        int n = rotation.size();
        int totalRounds = n - 1;

        for (int round = 1; round <= totalRounds; round++) {
            for (int i = 0; i < n / 2; i++) {
                Player player1 = rotation.get(i);
                Player player2 = rotation.get(n - 1 - i);

                if (player1 != null && player2 != null) {
                    createMatch(tournament, player1, player2, round, groupNumber, saveMatchPort);
                }
            }
            rotateExceptFirst(rotation);
        }
    }

    private static void rotateExceptFirst(List<Player> rotation) {
        int n = rotation.size();
        Player last = rotation.get(n - 1);
        for (int i = n - 1; i > 1; i--) {
            rotation.set(i, rotation.get(i - 1));
        }
        rotation.set(1, last);
    }

    private static void createMatch(Tournament tournament, Player player1, Player player2,
                                    int round, Integer groupNumber, SaveMatchPort saveMatchPort) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(round);
        match.setGroupNumber(groupNumber);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setStatus(MatchStatus.PENDING);
        saveMatchPort.saveMatch(match);
    }
}