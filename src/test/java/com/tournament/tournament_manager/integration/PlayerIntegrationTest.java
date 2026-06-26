package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.repository.PlayerRepository;
import com.tournament.tournament_manager.service.player.CreatePlayerService;
import com.tournament.tournament_manager.service.player.GetPlayerService;
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
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        PlayerResponse response = createPlayerService.createPlayer(request);

        assertEquals("toto", response.username());
        assertEquals(1000, response.eloRating());
        assertTrue(playerRepository.existsByUsername("toto"));
    }

    @Test
    void createPlayer_shouldHaveDefaultElo() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        PlayerResponse response = createPlayerService.createPlayer(request);
        assertEquals(1000, response.eloRating());
    }

    @Test
    void getPlayerById_shouldThrow_whenNotFound() {
        assertThrows(PlayerNotFoundException.class, () -> getPlayerService.getPlayerById(999L));
    }

    @Test
    void createPlayer_shouldThrow_whenUsernameAlreadyExists() {
        createPlayerService.createPlayer(new CreatePlayerRequest("toto", "toto@mail.com"));
        assertThrows(Exception.class, () ->
                createPlayerService.createPlayer(new CreatePlayerRequest("toto", "other@mail.com")));
    }
}