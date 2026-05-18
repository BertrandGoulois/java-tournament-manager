package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.*;
import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.dto.response.PlayerStatsResponse;
import com.tournament.tournament_manager.exception.PlayerAlreadyExistsException;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private LoadPlayerPort loadPlayerPort;
    @Mock
    private SavePlayerPort savePlayerPort;
    @Mock
    private ExistsPlayerPort existsPlayerPort;
    @Mock
    private LoadAllPlayersPort loadAllPlayersPort;
    @Mock
    private CountMatchesByPlayerPort countMatchesByPlayerPort;
    @Mock
    private LoadEloHistoryPort loadEloHistoryPort;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void createPlayer_shouldReturnPlayerResponse_whenValid() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        Player saved = new Player();
        saved.setUsername("toto");
        saved.setEmail("toto@mail.com");

        when(existsPlayerPort.existsByUsername("toto")).thenReturn(false);
        when(existsPlayerPort.existsByEmail("toto@mail.com")).thenReturn(false);
        when(savePlayerPort.savePlayer(any())).thenReturn(saved);

        PlayerResponse response = playerService.createPlayer(request);
        assertEquals("toto", response.username());
    }

    @Test
    void createPlayer_shouldThrow_whenUsernameAlreadyExists() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        when(existsPlayerPort.existsByUsername("toto")).thenReturn(true);

        assertThrows(PlayerAlreadyExistsException.class, () -> playerService.createPlayer(request));
    }

    @Test
    void createPlayer_shouldThrow_whenEmailAlreadyExists() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        when(existsPlayerPort.existsByUsername("toto")).thenReturn(false);
        when(existsPlayerPort.existsByEmail("toto@mail.com")).thenReturn(true);

        assertThrows(PlayerAlreadyExistsException.class, () -> playerService.createPlayer(request));
    }

    @Test
    void getPlayerById_shouldThrow_whenNotFound() {
        when(loadPlayerPort.loadPlayer(1L)).thenThrow(new PlayerNotFoundException(1L));
        assertThrows(PlayerNotFoundException.class, () -> playerService.getPlayerById(1L));
    }

    @Test
    void getPlayerById_shouldReturnPlayer_whenFound() {
        Player player = new Player();
        player.setUsername("toto");
        player.setEmail("toto@mail.com");

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);

        PlayerResponse response = playerService.getPlayerById(1L);
        assertEquals("toto", response.username());
    }

    @Test
    void getAllPlayers_shouldReturnList() {
        Player player = new Player();
        player.setUsername("toto");
        player.setEmail("toto@mail.com");

        when(loadAllPlayersPort.loadAllPlayers()).thenReturn(List.of(player));

        List<PlayerResponse> responses = playerService.getAllPlayers();
        assertEquals(1, responses.size());
        assertEquals("toto", responses.get(0).username());
    }

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

        PlayerStatsResponse stats = playerService.getPlayerStats(1L);

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

        PlayerStatsResponse stats = playerService.getPlayerStats(1L);

        assertEquals(0, stats.matchesPlayed());
        assertEquals(0.0, stats.winRate());
    }

    @Test
    void getPlayerStats_shouldThrow_whenNotFound() {
        when(loadPlayerPort.loadPlayer(99L)).thenThrow(new PlayerNotFoundException(99L));
        assertThrows(PlayerNotFoundException.class, () -> playerService.getPlayerStats(99L));
    }
}