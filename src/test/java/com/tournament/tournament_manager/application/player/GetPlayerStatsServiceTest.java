package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.EloHistory;
import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;
import com.tournament.tournament_manager.domain.port.out.player.CountMatchesByPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.domain.model.PlayerStats;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPlayerStatsServiceTest {

    @Mock
    private LoadPlayerPort loadPlayerPort;
    @Mock
    private CountMatchesByPlayerPort countMatchesByPlayerPort;
    @Mock
    private LoadEloHistoryPort loadEloHistoryPort;

    @InjectMocks
    private GetPlayerStatsService getPlayerStatsService;

    @Test
    void getPlayerStats_shouldReturnStats_whenMatchesPlayed() {
        Player player = new Player();
        player.setId(1L);
        player.setUsername("toto");
        player.setEmail("toto@mail.com");

        Match match = new Match();
        match.setId(1L);

        EloHistory history = new EloHistory();
        history.setEloChange(24);
        history.setEloAfter(1024);
        history.setMatch(match);

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(countMatchesByPlayerPort.countByPlayer(1L)).thenReturn(3L);
        when(countMatchesByPlayerPort.countWinsByPlayer(1L)).thenReturn(2L);
        when(loadEloHistoryPort.loadByPlayerIdOrderByDateDesc(1L)).thenReturn(List.of(history));

        PlayerStats stats = getPlayerStatsService.getPlayerStats(1L);

        assertEquals(3, stats.matchesPlayed());
        assertEquals(2, stats.wins());
        assertEquals(1, stats.losses());
        assertEquals(66.67, stats.winRate(), 0.01);
    }

    @Test
    void getPlayerStats_shouldReturnZeroWinRate_whenNoMatchesPlayed() {
        Player player = new Player();
        player.setId(1L);
        player.setUsername("player1");
        player.setEloRating(new EloRating(1000));

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(countMatchesByPlayerPort.countByPlayer(1L)).thenReturn(0L);
        when(countMatchesByPlayerPort.countWinsByPlayer(1L)).thenReturn(0L);
        when(loadEloHistoryPort.loadByPlayerIdOrderByDateDesc(1L)).thenReturn(List.of());

        PlayerStats response = getPlayerStatsService.getPlayerStats(1L);

        assertEquals(0.0, response.winRate());
        assertEquals(0, response.matchesPlayed());
        assertEquals(0, response.wins());
        assertEquals(0, response.losses());
    }

    @Test
    void getPlayerStats_shouldThrow_whenNotFound() {
        when(loadPlayerPort.loadPlayer(99L)).thenThrow(new PlayerNotFoundException(99L));
        assertThrows(PlayerNotFoundException.class, () -> getPlayerStatsService.getPlayerStats(99L));
    }

    @Test
    void getPlayerStats_shouldReturnCorrectWinRate_whenMatchesPlayed() {
        Player player = new Player();
        player.setId(1L);
        player.setUsername("player1");
        player.setEloRating(new EloRating(1000));

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(countMatchesByPlayerPort.countByPlayer(1L)).thenReturn(3L);
        when(countMatchesByPlayerPort.countWinsByPlayer(1L)).thenReturn(2L);
        when(loadEloHistoryPort.loadByPlayerIdOrderByDateDesc(1L)).thenReturn(List.of());

        PlayerStats response = getPlayerStatsService.getPlayerStats(1L);

        assertEquals(66.67, response.winRate());
        assertEquals(3, response.matchesPlayed());
        assertEquals(2, response.wins());
        assertEquals(1, response.losses());
    }

    @Test
    void getPlayerStats_shouldMapEloHistoryCorrectly() {
        Player player = new Player();
        player.setId(1L);
        player.setUsername("player1");
        player.setEloRating(new EloRating(1000));

        Match match = new Match();
        match.setId(5L);

        EloHistory history =
                new EloHistory();
        history.setEloChange(16);
        history.setEloAfter(1016);
        history.setMatch(match);

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(countMatchesByPlayerPort.countByPlayer(1L)).thenReturn(1L);
        when(countMatchesByPlayerPort.countWinsByPlayer(1L)).thenReturn(1L);
        when(loadEloHistoryPort.loadByPlayerIdOrderByDateDesc(1L)).thenReturn(List.of(history));

        PlayerStats response = getPlayerStatsService.getPlayerStats(1L);

        assertEquals(1, response.eloHistory().size());
        assertEquals(16, response.eloHistory().get(0).getEloChange());
        assertEquals(1016, response.eloHistory().get(0).getEloAfter());
        assertEquals(5L, response.eloHistory().get(0).getMatch().getId());
    }
}