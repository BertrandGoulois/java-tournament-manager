package com.tournament.tournament_manager.application.strategy.start;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tournament.tournament_manager.application.shared.RoundRobinUtils;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;

/**
 * Stratégie de démarrage pour le format {@link TournamentFormat#ROUND_ROBIN}.
 *
 * <p>Délègue la génération des matchs à {@link RoundRobinUtils}, sans notion
 * de groupe (tous les joueurs s'affrontent dans un seul round-robin global).
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
        RoundRobinUtils.generateRoundRobinMatches(tournament, players, null, saveMatchPort);
    }
}
