package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.out.player.LoadAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
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
class GetPlayerServiceTest {

    @Mock
    private LoadPlayerPort loadPlayerPort;
    @Mock
    private LoadAllPlayersPort loadAllPlayersPort;

    @InjectMocks
    private GetPlayerService getPlayerService;

    @Test
    void getPlayerById_shouldThrow_whenNotFound() {
        when(loadPlayerPort.loadPlayer(1L)).thenThrow(new PlayerNotFoundException(1L));
        assertThrows(PlayerNotFoundException.class, () -> getPlayerService.getPlayerById(1L));
    }

    @Test
    void getPlayerById_shouldReturnPlayer_whenFound() {
        Player player = new Player();
        player.setUsername("toto");
        player.setEmail("toto@mail.com");
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        PlayerResponse response = getPlayerService.getPlayerById(1L);
        assertEquals("toto", response.username());
    }

    @Test
    void getAllPlayers_shouldReturnList() {
        Player player = new Player();
        player.setUsername("toto");
        player.setEmail("toto@mail.com");
        PageResult<Player> page = PageResult.of(List.of(player), 0, 20, 1);
        when(loadAllPlayersPort.loadAllPlayers(any())).thenReturn(page);
        PageResult<PlayerResponse> responses = getPlayerService.getAllPlayers(
                com.tournament.tournament_manager.domain.model.PageRequest.of(0, 20));
        assertEquals(1, responses.totalElements());
        assertEquals("toto", responses.content().get(0).username());
    }
}