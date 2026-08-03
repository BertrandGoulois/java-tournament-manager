package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.in.tournament.GenerateKnockoutBracketFromGroupsUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.application.shared.BracketUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cas d'utilisation : transition entre la phase de groupes et la phase de
 * bracket pour un tournoi {@link com.tournament.tournament_manager.domain.model.enums.TournamentFormat#GROUPS_THEN_KNOCKOUT}.
 *
 * <p>Calcule le classement de chaque groupe (3 points par victoire, même règle
 * que {@link GetStandingsService}), retient les {@code tournament.getQualifiersPerGroup()}
 * meilleurs de chaque groupe, puis génère le bracket en élimination directe entre
 * tous les qualifiés via {@link BracketUtils}.
 *
 * <p>Ne déclenche la génération que si <strong>tous</strong> les matchs de groupe
 * (c'est-à-dire ceux dont {@code groupNumber != null}) sont {@code FINISHED}.
 * Idempotent : si le bracket a déjà été généré (présence de matchs avec
 * {@code groupNumber == null}), ne le régénère pas.
 */
@Service
@Transactional
public class GenerateKnockoutBracketFromGroupsService implements GenerateKnockoutBracketFromGroupsUseCase {

    private static final int POINTS_PER_WIN = 3;

    private final LoadMatchesByTournamentPort loadMatchesByTournamentPort;
    private final SaveMatchPort saveMatchPort;

    public GenerateKnockoutBracketFromGroupsService(LoadMatchesByTournamentPort loadMatchesByTournamentPort,
                                                    SaveMatchPort saveMatchPort) {
        this.loadMatchesByTournamentPort = loadMatchesByTournamentPort;
        this.saveMatchPort = saveMatchPort;
    }

    @Override
    public void checkGroupsCompletionAndGenerateBracket(Tournament tournament) {
        List<Match> allMatches = loadMatchesByTournamentPort.loadByTournamentId(tournament.getId());

        List<Match> groupMatches = allMatches.stream()
                .filter(m -> m.getGroupNumber() != null)
                .collect(Collectors.toList());

        boolean bracketAlreadyGenerated = allMatches.stream()
                .anyMatch(m -> m.getGroupNumber() == null);
        if (bracketAlreadyGenerated) {
            return;
        }

        boolean allGroupMatchesFinished = !groupMatches.isEmpty() && groupMatches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
        if (!allGroupMatchesFinished) {
            return;
        }

        List<Player> qualifiers = computeQualifiers(groupMatches, tournament.getQualifiersPerGroup());

        List<Player> seeded = BracketUtils.seedByElo(qualifiers);
        int playerCount = seeded.size();
        int bracketSize = BracketUtils.calculateFirstRound(playerCount);
        List<Integer> seedOrder = BracketUtils.seedOrder(bracketSize);

        for (int position = 0; position < bracketSize / 2; position++) {
            int seedA = seedOrder.get(position * 2);
            int seedB = seedOrder.get(position * 2 + 1);
            boolean aExists = seedA <= playerCount;
            boolean bExists = seedB <= playerCount;

            Player player1 = aExists ? seeded.get(seedA - 1) : seeded.get(seedB - 1);
            Player player2 = (aExists && bExists) ? seeded.get(seedB - 1) : null;

            BracketUtils.createMatch(tournament, player1, player2, bracketSize, position, saveMatchPort);
        }
    }

    /**
     * Calcule les qualifiés de chaque groupe selon le même barème de points
     * que le classement round-robin (3 points par victoire), et retient les
     * {@code qualifiersPerGroup} meilleurs de chaque groupe.
     *
     * @param groupMatches      tous les matchs de phase de groupes (tous groupes confondus)
     * @param qualifiersPerGroup nombre de qualifiés à retenir par groupe
     * @return la liste des joueurs qualifiés, tous groupes confondus
     */
    private List<Player> computeQualifiers(List<Match> groupMatches, int qualifiersPerGroup) {
        Map<Integer, Map<Long, PlayerPoints>> pointsByGroup = new HashMap<>();

        for (Match match : groupMatches) {
            int groupNumber = match.getGroupNumber();
            pointsByGroup.computeIfAbsent(groupNumber, g -> new LinkedHashMap<>());

            registerParticipant(pointsByGroup.get(groupNumber), match.getPlayer1());
            registerParticipant(pointsByGroup.get(groupNumber), match.getPlayer2());

            if (match.getWinner() != null) {
                pointsByGroup.get(groupNumber).get(match.getWinner().getId()).points += POINTS_PER_WIN;
            }
        }

        List<Player> qualifiers = new ArrayList<>();
        for (Map<Long, PlayerPoints> groupStandings : pointsByGroup.values()) {
            groupStandings.values().stream()
                    .sorted(Comparator.comparingInt((PlayerPoints pp) -> pp.points).reversed())
                    .limit(qualifiersPerGroup)
                    .forEach(pp -> qualifiers.add(pp.player));
        }
        return qualifiers;
    }

    private void registerParticipant(Map<Long, PlayerPoints> standings, Player player) {
        if (player == null) {
            return;
        }
        standings.computeIfAbsent(player.getId(), id -> new PlayerPoints(player));
    }

    private static class PlayerPoints {
        private final Player player;
        private int points;

        private PlayerPoints(Player player) {
            this.player = player;
        }
    }
}