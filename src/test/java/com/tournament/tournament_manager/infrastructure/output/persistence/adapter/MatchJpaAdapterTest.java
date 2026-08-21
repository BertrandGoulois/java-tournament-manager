package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.MatchMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.PlayerMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.TournamentMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.MatchRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

@ExtendWith(MockitoExtension.class)
class MatchJpaAdapterTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TournamentRepository tournamentRepository;

    private MatchJpaAdapter matchJpaAdapter;

    @BeforeEach
    void setUp() {
        PlayerMapper playerMapper = new PlayerMapper();
        TournamentMapper tournamentMapper = new TournamentMapper();
        MatchMapper matchMapper = new MatchMapper(playerMapper, tournamentMapper);
        matchJpaAdapter = new MatchJpaAdapter(matchRepository, playerRepository, tournamentRepository, matchMapper);
    }

    private MatchEntity entityWithId(long id) {
        MatchEntity entity = new MatchEntity();
        entity.setId(id);
        return entity;
    }

    @Test
    void loadMatch_shouldReturnMatch_whenFound() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(entityWithId(1L)));

        Match result = matchJpaAdapter.loadMatch(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void loadMatch_shouldThrow_whenNotFound() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MatchNotFoundException.class, () -> matchJpaAdapter.loadMatch(99L));
    }

    @Test
    void saveMatch_shouldResolveReferencesAndReturnSavedMatch_whenNoId() {
        Tournament tournament = Tournament.reconstitute(10L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, tournament, player1, player2, null);

        TournamentEntity tournamentEntityRef = new TournamentEntity();
        tournamentEntityRef.setName("Test Tournament");
        when(tournamentRepository.getReferenceById(10L)).thenReturn(tournamentEntityRef);
        when(playerRepository.getReferenceById(1L)).thenReturn(new PlayerEntity());
        when(playerRepository.getReferenceById(2L)).thenReturn(new PlayerEntity());
        when(matchRepository.save(any())).thenAnswer(inv -> {
            MatchEntity e = inv.getArgument(0);
            e.setId(42L);
            return e;
        });

        Match result = matchJpaAdapter.saveMatch(match);

        assertNotNull(result);
        assertEquals(42L, result.getId());
    }

    @Test
    void saveMatch_shouldHandleNullPlayer2_forBye() {
        Tournament tournament = Tournament.reconstitute(10L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Player player1 = new Player();
        player1.setId(1L);

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, tournament, player1, null, null);

        TournamentEntity tournamentEntityRef = new TournamentEntity();
        tournamentEntityRef.setName("Test Tournament");
        when(tournamentRepository.getReferenceById(10L)).thenReturn(tournamentEntityRef);
        when(playerRepository.getReferenceById(1L)).thenReturn(new PlayerEntity());
        when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Match result = matchJpaAdapter.saveMatch(match);

        assertNull(result.getPlayer2());
    }

    @Test
    void loadByTournamentIdAndRound_shouldReturnMatches() {
        when(matchRepository.findByTournamentIdAndRound(1L, 4)).thenReturn(List.of(entityWithId(1L)));

        List<Match> result = matchJpaAdapter.loadByTournamentIdAndRound(1L, 4);

        assertEquals(1, result.size());
    }

    @Test
    void loadByTournamentId_shouldReturnMatches() {
        when(matchRepository.findByTournamentId(1L)).thenReturn(List.of(entityWithId(1L)));

        List<Match> result = matchJpaAdapter.loadByTournamentId(1L);

        assertEquals(1, result.size());
    }
}
