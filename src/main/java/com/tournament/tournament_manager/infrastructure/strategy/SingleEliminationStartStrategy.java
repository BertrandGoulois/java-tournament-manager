package com.tournament.tournament_manager.infrastructure.strategy;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;
import com.tournament.tournament_manager.service.bracket.BracketUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Stratégie de démarrage pour le format {@link TournamentFormat#SINGLE_ELIMINATION}.
 *
 * <p>Mélange aléatoirement les joueurs et génère le premier tour du bracket.
 * Les joueurs sans adversaire (effectif impair) reçoivent un match {@code FINISHED}
 * immédiat (bye).
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

        for (int i = 0; i < players.size(); i += 2) {
            Player player1 = players.get(i);
            Player player2 = (i + 1 < players.size()) ? players.get(i + 1) : null;
            BracketUtils.createMatch(tournament, player1, player2,
                    BracketUtils.calculateFirstRound(players.size()), saveMatchPort);
        }
    }
}