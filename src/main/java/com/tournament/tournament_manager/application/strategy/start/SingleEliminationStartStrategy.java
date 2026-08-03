package com.tournament.tournament_manager.application.strategy.start;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;
import com.tournament.tournament_manager.application.shared.BracketUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stratégie de démarrage pour le format {@link TournamentFormat#SINGLE_ELIMINATION}.
 *
 * <p>Seede les joueurs par classement ELO (seed 1 = ELO le plus élevé) et génère le premier
 * tour du bracket selon l'ordre de seeding standard ({@link BracketUtils#seedOrder}) : les
 * meilleurs joueurs ne peuvent se rencontrer qu'au tour le plus tardif possible compte tenu
 * de leur rang, au lieu d'un tirage purement aléatoire.
 *
 * <p>Le nombre de byes nécessaires ({@code bracketSize - playerCount}, où
 * {@code bracketSize} est la plus petite puissance de 2 supérieure ou égale
 * à l'effectif) va aux joueurs les mieux classés — c'est la convention standard :
 * les meilleurs seeds sont récompensés par un tour gratuit.
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
        List<Player> seeded = BracketUtils.seedByElo(players);
        int playerCount = seeded.size();
        int bracketSize = BracketUtils.calculateFirstRound(playerCount);
        List<Integer> seedOrder = BracketUtils.seedOrder(bracketSize);

        for (int position = 0; position < bracketSize / 2; position++) {
            int seedA = seedOrder.get(position * 2);
            int seedB = seedOrder.get(position * 2 + 1);

            boolean aExists = seedA <= playerCount;
            boolean bExists = seedB <= playerCount;

            if (!aExists && !bExists) {
                // Ne devrait jamais arriver : le nombre de byes (bracketSize - playerCount)
                // est toujours strictement inférieur à bracketSize / 2, donc chaque paire de
                // slots contient au moins un joueur réel dans l'ordre de seeding standard.
                throw new IllegalStateException(
                        "Seeding invalide : aucun joueur réel aux positions " + (position * 2)
                                + " et " + (position * 2 + 1));
            }

            Player player1 = aExists ? seeded.get(seedA - 1) : seeded.get(seedB - 1);
            Player player2 = (aExists && bExists) ? seeded.get(seedB - 1) : null;

            BracketUtils.createMatch(tournament, player1, player2, bracketSize, position, saveMatchPort);
        }
    }
}
