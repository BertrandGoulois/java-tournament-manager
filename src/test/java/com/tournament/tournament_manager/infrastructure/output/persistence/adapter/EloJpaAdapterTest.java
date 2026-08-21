package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.EloHistory;
import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.EloHistoryMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.MatchMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.PlayerMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.TournamentMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.EloHistoryRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.MatchRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

@ExtendWith(MockitoExtension.class)
class EloJpaAdapterTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private EloHistoryRepository eloHistoryRepository;
    @Mock
    private MatchRepository matchRepository;

    private EloJpaAdapter eloJpaAdapter;

    @BeforeEach
    void setUp() {
        PlayerMapper playerMapper = new PlayerMapper();
        MatchMapper matchMapper = new MatchMapper(playerMapper, new TournamentMapper());
        eloJpaAdapter = new EloJpaAdapter(
                playerRepository, eloHistoryRepository, matchRepository,
                playerMapper, new EloHistoryMapper(playerMapper, matchMapper));
    }

    @Test
    void saveAllPlayers_shouldUpdateEachExistingEntity() {
        Player player = new Player();
        player.setId(1L);
        player.setUsername("updated");

        PlayerEntity existing = new PlayerEntity();
        existing.setId(1L);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        eloJpaAdapter.saveAllPlayers(List.of(player));

        verify(playerRepository, times(1)).save(existing);
        assertTrue(existing.getUsername().equals("updated"));
    }

    @Test
    void saveAllPlayers_shouldThrow_whenPlayerNotFound() {
        Player player = new Player();
        player.setId(99L);
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () -> eloJpaAdapter.saveAllPlayers(List.of(player)));
    }

    @Test
    void saveEloHistory_shouldResolveReferencesAndSave() {
        Player player = new Player();
        player.setId(1L);
        Match match = Match.reconstitute(2L, 0, 0, null, MatchStatus.PENDING, null, null, null, null, null, null);

        EloHistory history = new EloHistory();
        history.setPlayer(player);
        history.setMatch(match);

        when(playerRepository.getReferenceById(1L)).thenReturn(new PlayerEntity());
        when(matchRepository.getReferenceById(2L)).thenReturn(new MatchEntity());

        eloJpaAdapter.saveEloHistory(history);

        verify(eloHistoryRepository, times(1)).save(any());
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
