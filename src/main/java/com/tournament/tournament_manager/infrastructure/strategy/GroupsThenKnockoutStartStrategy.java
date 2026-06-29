package com.tournament.tournament_manager.infrastructure.strategy;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;
import com.tournament.tournament_manager.service.shared.RoundRobinUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stratégie de démarrage pour le format {@link TournamentFormat#GROUPS_THEN_KNOCKOUT}.
 *
 * <p>Répartit les joueurs en {@code tournament.getNumberOfGroups()} groupes équilibrés
 * (l'effectif total est garanti divisible par le nombre de groupes, validé à la création
 * du tournoi). Chaque groupe joue un round-robin complet via {@link RoundRobinUtils},
 * identifié par son {@code groupNumber}. Le bracket final entre qualifiés n'est généré
 * qu'une fois tous les matchs de groupe terminés (cf. {@code BracketListener}).
 */
@Component
public class GroupsThenKnockoutStartStrategy implements TournamentStartStrategy {

    private final SaveMatchPort saveMatchPort;

    public GroupsThenKnockoutStartStrategy(SaveMatchPort saveMatchPort) {
        this.saveMatchPort = saveMatchPort;
    }

    @Override
    public TournamentFormat supportedFormat() {
        return TournamentFormat.GROUPS_THEN_KNOCKOUT;
    }

    @Override
    public void generateInitialMatches(Tournament tournament, List<Player> players) {
        int numberOfGroups = tournament.getNumberOfGroups();

        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        List<List<Player>> groups = splitIntoGroups(shuffled, numberOfGroups);

        for (int i = 0; i < groups.size(); i++) {
            int groupNumber = i + 1;
            RoundRobinUtils.generateRoundRobinMatches(tournament, groups.get(i), groupNumber, saveMatchPort);
        }
    }

    /**
     * Répartit une liste de joueurs en {@code numberOfGroups} groupes de taille égale.
     *
     * @param players       joueurs déjà mélangés
     * @param numberOfGroups nombre de groupes (l'effectif doit être divisible par cette valeur)
     * @return liste de groupes de joueurs
     */
    private List<List<Player>> splitIntoGroups(List<Player> players, int numberOfGroups) {
        int groupSize = players.size() / numberOfGroups;
        List<List<Player>> groups = new ArrayList<>();
        for (int g = 0; g < numberOfGroups; g++) {
            int from = g * groupSize;
            int to = from + groupSize;
            groups.add(new ArrayList<>(players.subList(from, to)));
        }
        return groups;
    }
}