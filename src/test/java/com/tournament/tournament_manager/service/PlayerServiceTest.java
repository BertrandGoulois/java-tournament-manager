package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.exception.PlayerAlreadyExistsException;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.repository.EloHistoryRepository;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private EloHistoryRepository eloHistoryRepository;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void createPlayer_shouldReturnPlayerResponse_whenValid() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        Player saved = new Player();
        saved.setUsername("toto");
        saved.setEmail("toto@mail.com");

        when(playerRepository.existsByUsername("toto")).thenReturn(false);
        when(playerRepository.existsByEmail("toto@mail.com")).thenReturn(false);
        when(playerRepository.save(any())).thenReturn(saved);

        PlayerResponse response = playerService.createPlayer(request);

        assertEquals("toto", response.username());
    }

    @Test
    void createPlayer_shouldThrow_whenUsernameAlreadyExists() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        when(playerRepository.existsByUsername("toto")).thenReturn(true);

        assertThrows(PlayerAlreadyExistsException.class, () -> playerService.createPlayer(request));
    }

    @Test
    void createPlayer_shouldThrow_whenEmailAlreadyExists() {
        CreatePlayerRequest request = new CreatePlayerRequest("toto", "toto@mail.com");
        when(playerRepository.existsByUsername("toto")).thenReturn(false);
        when(playerRepository.existsByEmail("toto@mail.com")).thenReturn(true);

        assertThrows(PlayerAlreadyExistsException.class, () -> playerService.createPlayer(request));
    }

    @Test
    void getPlayerById_shouldThrow_whenNotFound() {
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(PlayerNotFoundException.class, () -> playerService.getPlayerById(1L));
    }

    @Test
    void getPlayerById_shouldReturnPlayer_whenFound() {
        Player player = new Player();
        player.setUsername("toto");
        player.setEmail("toto@mail.com");

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        PlayerResponse response = playerService.getPlayerById(1L);
        assertEquals("toto", response.username());
    }
}