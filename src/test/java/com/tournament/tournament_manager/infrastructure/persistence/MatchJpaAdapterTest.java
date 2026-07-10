package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchJpaAdapterTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchJpaAdapter matchJpaAdapter;

    @Test
    void loadMatch_shouldReturnMatch_whenFound() {
        Match match = new Match();
        match.setId(1L);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        Match result = matchJpaAdapter.loadMatch(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void loadMatch_shouldThrow_whenNotFound() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MatchNotFoundException.class, () -> matchJpaAdapter.loadMatch(99L));
    }

    @Test
    void saveMatch_shouldReturnSavedMatch() {
        Match match = new Match();
        when(matchRepository.save(any())).thenReturn(match);

        Match result = matchJpaAdapter.saveMatch(match);

        assertNotNull(result);
    }

    @Test
    void loadByTournamentIdAndRound_shouldReturnMatches() {
        Match match = new Match();
        when(matchRepository.findByTournamentIdAndRound(1L, 4)).thenReturn(List.of(match));

        List<Match> result = matchJpaAdapter.loadByTournamentIdAndRound(1L, 4);

        assertEquals(1, result.size());
    }

    @Test
    void loadByTournamentId_shouldReturnMatches() {
        Match match = new Match();
        when(matchRepository.findByTournamentId(1L)).thenReturn(List.of(match));

        List<Match> result = matchJpaAdapter.loadByTournamentId(1L);

        assertEquals(1, result.size());
    }
}