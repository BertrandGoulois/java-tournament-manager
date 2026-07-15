package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.player.ExistsPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.SavePlayerPort;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.exception.domain.PlayerAlreadyExistsException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePlayerServiceTest {

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();
    @Mock
    private SavePlayerPort savePlayerPort;
    @Mock
    private ExistsPlayerPort existsPlayerPort;

    private CreatePlayerService createPlayerService;

    @BeforeEach
    void setUp(){
        createPlayerService = new CreatePlayerService(savePlayerPort, existsPlayerPort, meterRegistry);
    }

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