package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
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
class TournamentJpaAdapterTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private TournamentJpaAdapter tournamentJpaAdapter;

    @Test
    void loadTournament_shouldReturnTournament_whenFound() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        Tournament result = tournamentJpaAdapter.loadTournament(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void loadTournament_shouldThrow_whenNotFound() {
        when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TournamentNotFoundException.class, () -> tournamentJpaAdapter.loadTournament(99L));
    }

    @Test
    void saveTournament_shouldReturnSavedTournament() {
        Tournament tournament = new Tournament();
        when(tournamentRepository.save(any())).thenReturn(tournament);

        Tournament result = tournamentJpaAdapter.saveTournament(tournament);

        assertNotNull(result);
    }

    @Test
    void existsByName_shouldReturnTrue_whenExists() {
        when(tournamentRepository.existsByName("Test")).thenReturn(true);

        assertTrue(tournamentJpaAdapter.existsByName("Test"));
    }

    @Test
    void existsByName_shouldReturnFalse_whenNotExists() {
        when(tournamentRepository.existsByName("Test")).thenReturn(false);

        assertFalse(tournamentJpaAdapter.existsByName("Test"));
    }

    @Test
    void loadAllTournaments_shouldReturnPage() {
        Tournament tournament = new Tournament();
        Page<Tournament> page = new PageImpl<>(List.of(tournament));
        when(tournamentRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Tournament> result = tournamentJpaAdapter.loadAllTournaments(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }
}