package com.tournament.tournament_manager.application.strategy.start;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;
import com.tournament.tournament_manager.application.shared.BracketUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Stratégie de démarrage pour le format {@link TournamentFormat#SINGLE_ELIMINATION}.
 *
 * <p>Mélange aléatoirement les joueurs et génère le premier tour du bracket.
 * Le nombre de byes nécessaires ({@code bracketSize - playerCount}, où
 * {@code bracketSize} est la plus petite puissance de 2 supérieure ou égale
 * à l'effectif) est calculé une seule fois et distribué entièrement au
 * premier tour, garantissant que tous les tours suivants opposent un
 * nombre de joueurs strictement pair — aucun bye ne peut apparaître
 * au-delà du premier tour.
 */
@Component
public class SingleEliminationStartStrategy implements TournamentStartStrategy {

    private final SaveMatchPort saveMatchPort;

    public SingleEliminationStartStrategy(SaveMatchPort saveMatchPort) {
        this.saveMatchPort = saveMatchPort;
    }

    @Override
    public TournamentFormat supportedFormat() {
        return TournamentFormat.SINGLE_ELIMINATION;
    }

    @Override
    public void generateInitialMatches(Tournament tournament, List<Player> players) {
        Collections.shuffle(players);

        int playerCount = players.size();
        int bracketSize = BracketUtils.calculateFirstRound(playerCount);
        int byeCount = bracketSize - playerCount;

        List<Player> byePlayers = players.subList(0, byeCount);
        List<Player> matchPlayers = players.subList(byeCount, playerCount);

        for (Player player : byePlayers) {
            BracketUtils.createMatch(tournament, player, null, bracketSize, saveMatchPort);
        }

        for (int i = 0; i < matchPlayers.size(); i += 2) {
            BracketUtils.createMatch(tournament, matchPlayers.get(i), matchPlayers.get(i + 1),
                    bracketSize, saveMatchPort);
        }
    }
}