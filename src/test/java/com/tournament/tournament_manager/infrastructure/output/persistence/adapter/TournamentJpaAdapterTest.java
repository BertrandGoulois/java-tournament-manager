package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.TournamentMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;

@ExtendWith(MockitoExtension.class)
class TournamentJpaAdapterTest {

    @Mock
    private TournamentRepository tournamentRepository;

    private TournamentJpaAdapter tournamentJpaAdapter;

    @BeforeEach
    void setUp() {
        tournamentJpaAdapter = new TournamentJpaAdapter(tournamentRepository, new TournamentMapper());
    }

    private TournamentEntity entityWithId(long id) {
        TournamentEntity entity = new TournamentEntity();
        entity.setId(id);
        entity.setName("tournament" + id);
        return entity;
    }

    @Test
    void loadTournament_shouldReturnTournament_whenFound() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(entityWithId(1L)));

        Tournament result = tournamentJpaAdapter.loadTournament(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void loadTournament_shouldThrow_whenNotFound() {
        when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TournamentNotFoundException.class, () -> tournamentJpaAdapter.loadTournament(99L));
    }

    @Test
    void saveTournament_shouldCreateNewEntity_whenNoId() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("new tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        when(tournamentRepository.save(any())).thenAnswer(inv -> {
            TournamentEntity e = inv.getArgument(0);
            e.setId(42L);
            return e;
        });

        Tournament result = tournamentJpaAdapter.saveTournament(tournament);

        assertNotNull(result);
        assertEquals(42L, result.getId());
    }

    @Test
    void saveTournament_shouldUpdateExistingEntity_whenIdPresent() {
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("updated"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        TournamentEntity existing = entityWithId(1L);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tournamentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tournament result = tournamentJpaAdapter.saveTournament(tournament);

        assertEquals("updated", result.getName().value());
        assertEquals("updated", existing.getName());
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
        Page<TournamentEntity> page = new PageImpl<>(List.of(entityWithId(1L)));
        when(tournamentRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResult<Tournament> result = tournamentJpaAdapter.loadAllTournaments(
                com.tournament.tournament_manager.domain.model.PageRequest.of(0, 20));

        assertEquals(1, result.totalElements());
    }
}
