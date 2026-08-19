package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.domain.model.CreatePlayerCommand;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.application.player.CreatePlayerService;
import com.tournament.tournament_manager.application.player.GetPlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlayerIntegrationTest {

    @Autowired
    private CreatePlayerService createPlayerService;

    @Autowired
    private GetPlayerService getPlayerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void createPlayer_shouldPersistInDatabase() {
        CreatePlayerCommand command = new CreatePlayerCommand("toto", "toto@mail.com");
        Player result = createPlayerService.createPlayer(command);

        assertEquals("toto", result.getUsername());
        assertEquals(1000, result.getEloRating().value());
        assertTrue(playerRepository.existsByUsername("toto"));
    }

    @Test
    void createPlayer_shouldHaveDefaultElo() {
        CreatePlayerCommand command = new CreatePlayerCommand("toto", "toto@mail.com");
        Player result = createPlayerService.createPlayer(command);
        assertEquals(1000, result.getEloRating().value());
    }

    @Test
    void getPlayerById_shouldThrow_whenNotFound() {
        assertThrows(PlayerNotFoundException.class, () -> getPlayerService.getPlayerById(999L));
    }

    @Test
    void createPlayer_shouldThrow_whenUsernameAlreadyExists() {
        createPlayerService.createPlayer(new CreatePlayerCommand("toto", "toto@mail.com"));
        assertThrows(Exception.class, () ->
                createPlayerService.createPlayer(new CreatePlayerCommand("toto", "other@mail.com")));
    }
}
