package com.tournament.tournament_manager.service.player;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.player.CountMatchesByPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.dto.response.player.PlayerStatsResponse;
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

        PlayerStatsResponse stats = getPlayerStatsService.getPlayerStats(1L);

        assertEquals(3, stats.matchesPlayed());
        assertEquals(2, stats.wins());
        assertEquals(1, stats.losses());
        assertEquals(66.67, stats.winRate(), 0.01);
    }

    @Test
    void getPlayerStats_shouldReturnZeroWinRate_whenNoMatchesPlayed() {
        Player player = new Player();
        player.setId(1L);
        player.setUsername("toto");
        player.setEmail("toto@mail.com");

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(countMatchesByPlayerPort.countByPlayer(1L)).thenReturn(0L);
        when(countMatchesByPlayerPort.countWinsByPlayer(1L)).thenReturn(0L);
        when(loadEloHistoryPort.loadByPlayerIdOrderByDateDesc(1L)).thenReturn(List.of());

        PlayerStatsResponse stats = getPlayerStatsService.getPlayerStats(1L);

        assertEquals(0, stats.matchesPlayed());
        assertEquals(0.0, stats.winRate());
    }

    @Test
    void getPlayerStats_shouldThrow_whenNotFound() {
        when(loadPlayerPort.loadPlayer(99L)).thenThrow(new PlayerNotFoundException(99L));
        assertThrows(PlayerNotFoundException.class, () -> getPlayerStatsService.getPlayerStats(99L));
    }
}