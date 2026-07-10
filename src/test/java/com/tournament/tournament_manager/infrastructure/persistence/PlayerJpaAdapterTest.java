package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import com.tournament.tournament_manager.repository.EloHistoryRepository;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerJpaAdapterTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private EloHistoryRepository eloHistoryRepository;

    @InjectMocks
    private PlayerJpaAdapter playerJpaAdapter;

    @Test
    void loadPlayer_shouldReturnPlayer_whenFound() {
        Player player = new Player();
        player.setId(1L);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        Player result = playerJpaAdapter.loadPlayer(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void loadPlayer_shouldThrow_whenNotFound() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () -> playerJpaAdapter.loadPlayer(99L));
    }

    @Test
    void savePlayer_shouldReturnSavedPlayer() {
        Player player = new Player();
        when(playerRepository.save(any())).thenReturn(player);

        Player result = playerJpaAdapter.savePlayer(player);

        assertNotNull(result);
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenExists() {
        when(playerRepository.existsByUsername("toto")).thenReturn(true);

        assertTrue(playerJpaAdapter.existsByUsername("toto"));
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenNotExists() {
        when(playerRepository.existsByEmail("toto@mail.com")).thenReturn(false);

        assertFalse(playerJpaAdapter.existsByEmail("toto@mail.com"));
    }

    @Test
    void loadAllPlayers_shouldReturnPage() {
        Player player = new Player();
        Page<Player> page = new PageImpl<>(List.of(player));
        when(playerRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Player> result = playerJpaAdapter.loadAllPlayers(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void countByPlayer_shouldReturnCount() {
        when(matchRepository.countByPlayer1IdOrPlayer2Id(1L, 1L)).thenReturn(3L);

        assertEquals(3L, playerJpaAdapter.countByPlayer(1L));
    }

    @Test
    void countWinsByPlayer_shouldReturnCount() {
        when(matchRepository.countByWinnerId(1L)).thenReturn(2L);

        assertEquals(2L, playerJpaAdapter.countWinsByPlayer(1L));
    }

    @Test
    void loadByPlayerIdOrderByDateDesc_shouldReturnHistory() {
        EloHistory history = new EloHistory();
        when(eloHistoryRepository.findByPlayerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(history));

        List<EloHistory> result = playerJpaAdapter.loadByPlayerIdOrderByDateDesc(1L);

        assertEquals(1, result.size());
    }
}