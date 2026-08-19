package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.out.player.ExistsPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.SavePlayerPort;
import com.tournament.tournament_manager.domain.model.CreatePlayerCommand;
import com.tournament.tournament_manager.exception.domain.PlayerAlreadyExistsException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    void createPlayer_shouldReturnPlayer_whenValid() {
        CreatePlayerCommand request = new CreatePlayerCommand("toto", "toto@mail.com");
        Player saved = new Player();
        saved.setUsername("toto");
        saved.setEmail("toto@mail.com");

        when(existsPlayerPort.existsByUsername("toto")).thenReturn(false);
        when(existsPlayerPort.existsByEmail("toto@mail.com")).thenReturn(false);
        when(savePlayerPort.savePlayer(any())).thenReturn(saved);

        Player result = createPlayerService.createPlayer(request);
        assertEquals("toto", result.getUsername());
    }

    @Test
    void createPlayer_shouldThrow_whenUsernameAlreadyExists() {
        CreatePlayerCommand request = new CreatePlayerCommand("toto", "toto@mail.com");
        when(existsPlayerPort.existsByUsername("toto")).thenReturn(true);
        assertThrows(PlayerAlreadyExistsException.class, () -> createPlayerService.createPlayer(request));
    }

    @Test
    void createPlayer_shouldThrow_whenEmailAlreadyExists() {
        CreatePlayerCommand request = new CreatePlayerCommand("toto", "toto@mail.com");
        when(existsPlayerPort.existsByUsername("toto")).thenReturn(false);
        when(existsPlayerPort.existsByEmail("toto@mail.com")).thenReturn(true);
        assertThrows(PlayerAlreadyExistsException.class, () -> createPlayerService.createPlayer(request));
    }

    @Test
    void createPlayer_shouldReturnCorrectUsernameAndEmail() {
        Player saved = new Player();
        saved.setId(1L);
        saved.setUsername("player1");
        saved.setEmail("player1@mail.com");

        when(existsPlayerPort.existsByUsername("player1")).thenReturn(false);
        when(existsPlayerPort.existsByEmail("player1@mail.com")).thenReturn(false);
        when(savePlayerPort.savePlayer(any())).thenReturn(saved);

        Player result = createPlayerService.createPlayer(
                new CreatePlayerCommand("player1", "player1@mail.com"));

        assertEquals("player1", result.getUsername());
        assertEquals("player1@mail.com", result.getEmail());
    }

    @Test
    void createPlayer_shouldSavePlayerWithCorrectUsernameAndEmail() {
        Player saved = new Player();
        saved.setUsername("player1");
        saved.setEmail("player1@mail.com");

        when(existsPlayerPort.existsByUsername("player1")).thenReturn(false);
        when(existsPlayerPort.existsByEmail("player1@mail.com")).thenReturn(false);

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        when(savePlayerPort.savePlayer(captor.capture())).thenReturn(saved);

        createPlayerService.createPlayer(new CreatePlayerCommand("player1", "player1@mail.com"));

        assertEquals("player1", captor.getValue().getUsername());
        assertEquals("player1@mail.com", captor.getValue().getEmail());
    }
}