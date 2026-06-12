package com.tournament.tournament_manager.service.player;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.player.ExistsPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.SavePlayerPort;
import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.exception.PlayerAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePlayerServiceTest {

    @Mock
    private SavePlayerPort savePlayerPort;
    @Mock
    private ExistsPlayerPort existsPlayerPort;

    @InjectMocks
    private CreatePlayerService createPlayerService;

    @Test
    void createPlayer_shouldReturnPlayerResponse_whenValid() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        Player saved = new Player();
        saved.setUsername("toto");
        saved.setEmail("toto@mail.com");

        when(existsPlayerPort.existsByUsername("toto")).thenReturn(false);
        when(existsPlayerPort.existsByEmail("toto@mail.com")).thenReturn(false);
        when(savePlayerPort.savePlayer(any())).thenReturn(saved);

        PlayerResponse response = createPlayerService.createPlayer(request);
        assertEquals("toto", response.username());
    }

    @Test
    void createPlayer_shouldThrow_whenUsernameAlreadyExists() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        when(existsPlayerPort.existsByUsername("toto")).thenReturn(true);
        assertThrows(PlayerAlreadyExistsException.class, () -> createPlayerService.createPlayer(request));
    }

    @Test
    void createPlayer_shouldThrow_whenEmailAlreadyExists() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        when(existsPlayerPort.existsByUsername("toto")).thenReturn(false);
        when(existsPlayerPort.existsByEmail("toto@mail.com")).thenReturn(true);
        assertThrows(PlayerAlreadyExistsException.class, () -> createPlayerService.createPlayer(request));
    }
}