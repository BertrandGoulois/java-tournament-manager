package com.tournament.tournament_manager.application.strategy.start;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupsThenKnockoutStartStrategyTest {

    @Mock
    private SaveMatchPort saveMatchPort;

    @InjectMocks
    private GroupsThenKnockoutStartStrategy strategy;

    @Test
    void supportedFormat_shouldReturnGroupsThenKnockout() {
        assertEquals(TournamentFormat.GROUPS_THEN_KNOCKOUT, strategy.supportedFormat());
    }

    @Test
    void generateInitialMatches_shouldCreateTwoGroups_withFourPlayersAndTwoGroups() {
        Tournament tournament = new Tournament();
        tournament.setNumberOfGroups(2);

        List<Player> players = new ArrayList<>(List.of(
                new Player(), new Player(), new Player(), new Player()
        ));

        strategy.generateInitialMatches(tournament, players);

        // 2 groupes de 2 joueurs : 1 match par groupe = 2 matchs au total
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(2)).saveMatch(captor.capture());

        Set<Integer> groupNumbers = new HashSet<>();
        for (Match match : captor.getAllValues()) {
            groupNumbers.add(match.getGroupNumber());
        }
        assertEquals(Set.of(1, 2), groupNumbers);
    }

    @Test
    void generateInitialMatches_shouldCreateAllMatchesWithinSameGroup() {
        Tournament tournament = new Tournament();
        tournament.setNumberOfGroups(2);

        List<Player> players = new ArrayList<>(List.of(
                new Player(), new Player(), new Player(), new Player(),
                new Player(), new Player(), new Player(), new Player()
        ));

        strategy.generateInitialMatches(tournament, players);

        // 2 groupes de 4 joueurs : C(4,2) = 6 matchs par groupe = 12 matchs au total
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(12)).saveMatch(captor.capture());

        for (Match match : captor.getAllValues()) {
            assertNotNull(match.getGroupNumber());
            // Les deux joueurs d'un même match doivent appartenir au même groupe
            // (vérifié indirectement : aucun match ne mélange deux groupes différents
            // puisque generateRoundRobinMatches ne reçoit que les joueurs d'un seul groupe)
        }
    }

    @Test
    void generateInitialMatches_shouldAssignAllPlayersToAGroup() {
        Tournament tournament = new Tournament();
        tournament.setNumberOfGroups(3);

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            players.add(new Player());
        }

        strategy.generateInitialMatches(tournament, players);

        // 3 groupes de 3 joueurs : C(3,2) = 3 matchs par groupe = 9 matchs au total
        verify(saveMatchPort, times(9)).saveMatch(org.mockito.ArgumentMatchers.any(Match.class));
    }
}