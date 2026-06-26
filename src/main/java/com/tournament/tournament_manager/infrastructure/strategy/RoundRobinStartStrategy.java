package com.tournament.tournament_manager.infrastructure.strategy;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stratégie de démarrage pour le format {@link TournamentFormat#ROUND_ROBIN}.
 *
 * <p>Génère l'intégralité des matchs en une seule fois grâce à la méthode du cercle :
 * un joueur reste fixe, les autres tournent à chaque round, garantissant que chaque
 * paire de joueurs se rencontre exactement une fois. Le round correspond ici au
 * numéro de journée (1, 2, 3...), pas à un système d'élimination.
 *
 * <p>Si le nombre de joueurs est impair, un joueur "fantôme" (bye) est ajouté :
 * le joueur qui lui est associé à un round donné ne joue pas ce round-là.
 */
@Component
public class RoundRobinStartStrategy implements TournamentStartStrategy {

    private final SaveMatchPort saveMatchPort;

    public RoundRobinStartStrategy(SaveMatchPort saveMatchPort) {
        this.saveMatchPort = saveMatchPort;
    }

    @Override
    public TournamentFormat supportedFormat() {
        return TournamentFormat.ROUND_ROBIN;
    }

    @Override
    public void generateInitialMatches(Tournament tournament, List<Player> players) {
        List<Player> rotation = new ArrayList<>(players);
        Collections.shuffle(rotation);

        boolean hasGhost = rotation.size() % 2 != 0;
        if (hasGhost) {
            rotation.add(null); // joueur fantôme pour gérer l'effectif impair
        }

        int n = rotation.size();
        int totalRounds = n - 1;

        for (int round = 1; round <= totalRounds; round++) {
            for (int i = 0; i < n / 2; i++) {
                Player player1 = rotation.get(i);
                Player player2 = rotation.get(n - 1 - i);

                if (player1 != null && player2 != null) {
                    createRoundRobinMatch(tournament, player1, player2, round);
                }
            }
            rotateExceptFirst(rotation);
        }
    }

    /**
     * Fait tourner tous les joueurs d'une position vers la droite, sauf le premier
     * qui reste fixe (méthode du cercle).
     *
     * @param rotation la liste des joueurs (mutée en place)
     */
    private void rotateExceptFirst(List<Player> rotation) {
        int n = rotation.size();
        Player last = rotation.get(n - 1);
        for (int i = n - 1; i > 1; i--) {
            rotation.set(i, rotation.get(i - 1));
        }
        rotation.set(1, last);
    }

    private void createRoundRobinMatch(Tournament tournament, Player player1, Player player2, int round) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(round);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setStatus(MatchStatus.PENDING);
        saveMatchPort.saveMatch(match);
    }
}