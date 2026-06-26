package com.tournament.tournament_manager.service.player;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.player.LoadAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
        Page<Player> page = new PageImpl<>(List.of(player));
        when(loadAllPlayersPort.loadAllPlayers(any(Pageable.class))).thenReturn(page);
        Page<PlayerResponse> responses = getPlayerService.getAllPlayers(Pageable.unpaged());
        assertEquals(1, responses.getTotalElements());
        assertEquals("toto", responses.getContent().get(0).username());
    }
}