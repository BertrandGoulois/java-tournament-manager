package com.tournament.tournament_manager.application.strategy.start;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoundRobinStartStrategyTest {

    @Mock
    private SaveMatchPort saveMatchPort;

    @InjectMocks
    private RoundRobinStartStrategy strategy;

    @Test
    void supportedFormat_shouldReturnRoundRobin() {
        assertEquals(TournamentFormat.ROUND_ROBIN, strategy.supportedFormat());
    }

    @Test
    void generateInitialMatches_shouldCreateAllPairsExactlyOnce_withFourPlayers() {
        Player p1 = new Player();
        Player p2 = new Player();
        Player p3 = new Player();
        Player p4 = new Player();
        List<Player> players = List.of(p1, p2, p3, p4);

        Tournament tournament = new Tournament();
        strategy.generateInitialMatches(tournament, players);

        // 4 joueurs : C(4,2) = 6 matchs attendus
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(6)).saveMatch(captor.capture());

        Set<Set<Player>> pairs = new HashSet<>();
        for (Match match : captor.getAllValues()) {
            pairs.add(Set.of(match.getPlayer1(), match.getPlayer2()));
        }

        assertEquals(6, pairs.size(), "Chaque paire doit être unique");
    }

    @Test
    void generateInitialMatches_shouldHandleOddNumberOfPlayers_withoutCrashing() {
        Player p1 = new Player();
        Player p2 = new Player();
        Player p3 = new Player();
        List<Player> players = List.of(p1, p2, p3);

        Tournament tournament = new Tournament();
        strategy.generateInitialMatches(tournament, players);

        // 3 joueurs : C(3,2) = 3 matchs attendus, le bye n'est jamais persisté
        verify(saveMatchPort, times(3)).saveMatch(any());
    }

    @Test
    void generateInitialMatches_shouldSetAllMatchesAsPending() {
        Player p1 = new Player();
        Player p2 = new Player();
        List<Player> players = List.of(p1, p2);

        Tournament tournament = new Tournament();
        strategy.generateInitialMatches(tournament, players);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(1)).saveMatch(captor.capture());
        assertEquals(MatchStatus.PENDING, captor.getValue().getStatus());
    }
}