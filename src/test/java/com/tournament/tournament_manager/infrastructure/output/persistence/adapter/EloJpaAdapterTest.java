package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.EloHistoryRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloJpaAdapterTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private EloHistoryRepository eloHistoryRepository;

    @InjectMocks
    private EloJpaAdapter eloJpaAdapter;

    @Test
    void saveAllPlayers_shouldCallSaveAll() {
        Player player = new Player();
        eloJpaAdapter.saveAllPlayers(List.of(player));

        verify(playerRepository, times(1)).saveAll(List.of(player));
    }

    @Test
    void saveEloHistory_shouldCallSave() {
        EloHistory history = new EloHistory();
        eloJpaAdapter.saveEloHistory(history);

        verify(eloHistoryRepository, times(1)).save(history);
    }

    @Test
    void existsByMatchId_shouldReturnTrue_whenExists() {
        when(eloHistoryRepository.existsByMatchId(1L)).thenReturn(true);

        assertTrue(eloJpaAdapter.existsByMatchId(1L));
    }

    @Test
    void existsByMatchId_shouldReturnFalse_whenNotExists() {
        when(eloHistoryRepository.existsByMatchId(1L)).thenReturn(false);

        assertFalse(eloJpaAdapter.existsByMatchId(1L));
    }
}