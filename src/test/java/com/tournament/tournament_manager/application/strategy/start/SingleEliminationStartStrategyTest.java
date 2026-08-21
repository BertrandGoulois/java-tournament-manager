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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;

@ExtendWith(MockitoExtension.class)
class SingleEliminationStartStrategyTest {

    @Mock
    private SaveMatchPort saveMatchPort;

    @InjectMocks
    private SingleEliminationStartStrategy strategy;

    @Test
    void supportedFormat_shouldReturnSingleElimination() {
        assertEquals(TournamentFormat.SINGLE_ELIMINATION, strategy.supportedFormat());
    }

    @Test
    void generateInitialMatches_shouldCreateOneMatch_whenTwoPlayers() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        List<Player> players = new ArrayList<>(List.of(new Player(), new Player()));

        strategy.generateInitialMatches(tournament, players);

        verify(saveMatchPort, times(1)).saveMatch(any(Match.class));
    }

    @Test
    void generateInitialMatches_shouldCreateTwoMatches_whenFourPlayers() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        List<Player> players = new ArrayList<>(List.of(new Player(), new Player(), new Player(), new Player()));

        strategy.generateInitialMatches(tournament, players);

        verify(saveMatchPort, times(2)).saveMatch(any(Match.class));
    }

    @Test
    void generateInitialMatches_shouldCreateByeMatch_whenOddNumberOfPlayers() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        List<Player> players = new ArrayList<>(List.of(new Player(), new Player(), new Player()));

        strategy.generateInitialMatches(tournament, players);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(2)).saveMatch(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(m -> m.getPlayer2() == null));
    }

    @Test
    void generateInitialMatches_shouldCreateExactlyOneBye_whenFivePlayers() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        List<Player> players = new ArrayList<>(List.of(
                new Player(), new Player(), new Player(), new Player(), new Player()));

        strategy.generateInitialMatches(tournament, players);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);

        verify(saveMatchPort, times(4)).saveMatch(captor.capture());

        long byeCount = captor.getAllValues().stream().filter(m -> m.getPlayer2() == null).count();
        long realMatchCount = captor.getAllValues().stream().filter(m -> m.getPlayer2() != null).count();

        assertEquals(3, byeCount, "5 joueurs -> 3 byes attendus au premier tour (bracketSize=8)");
        assertEquals(1, realMatchCount, "5 joueurs -> 1 seul match reel au premier tour");
    }

    @Test
    void generateInitialMatches_shouldNeverPairTopTwoEloPlayers_atFirstRound() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        List<Player> players = new ArrayList<>();
        // 8 joueurs, ELO décroissant : p0 = meilleur, p1 = deuxième meilleur.
        for (int i = 0; i < 8; i++) {
            Player p = new Player();
            p.setId((long) i);
            p.setEloRating(new com.tournament.tournament_manager.domain.model.valueobjects.EloRating(2000 - i * 50));
            players.add(p);
        }
        Player best = players.get(0);
        Player secondBest = players.get(1);

        strategy.generateInitialMatches(tournament, players);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(4)).saveMatch(captor.capture());

        boolean facedEachOther = captor.getAllValues().stream().anyMatch(m ->
                (m.getPlayer1() == best && m.getPlayer2() == secondBest)
                        || (m.getPlayer1() == secondBest && m.getPlayer2() == best));

        assertTrue(!facedEachOther, "les deux meilleurs ELO ne doivent jamais s'affronter au premier tour");
    }

    @Test
    void generateInitialMatches_bestEloPlayer_shouldGetABye_whenByesAreNeeded() {
        // 5 joueurs -> bracketSize=8 -> 3 byes. Convention standard : les meilleurs seeds
        // reçoivent les byes en priorité.
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Player p = new Player();
            p.setId((long) i);
            p.setEloRating(new com.tournament.tournament_manager.domain.model.valueobjects.EloRating(2000 - i * 50));
            players.add(p);
        }
        Player best = players.get(0);

        strategy.generateInitialMatches(tournament, players);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(4)).saveMatch(captor.capture());

        boolean bestGotBye = captor.getAllValues().stream()
                .anyMatch(m -> m.getPlayer1() == best && m.getPlayer2() == null);
        assertTrue(bestGotBye, "le meilleur ELO doit recevoir un bye en priorité");
    }
}