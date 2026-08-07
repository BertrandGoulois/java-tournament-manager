package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.SoftDeletePlayerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePlayerServiceTest {

    @Mock
    private LoadPlayerPort loadPlayerPort;
    @Mock
    private SoftDeletePlayerPort softDeletePlayerPort;

    @InjectMocks
    private DeletePlayerService deletePlayerService;

    @Test
    void deletePlayer_shouldSetDeletedTrue() {
        Player player = new Player();
        player.setId(1L);

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);

        deletePlayerService.deletePlayer(1L);

        assertTrue(player.isDeleted());
        assertNotNull(player.getDeletedAt());
        verify(softDeletePlayerPort, times(1)).softDeletePlayer(player);
    }
}