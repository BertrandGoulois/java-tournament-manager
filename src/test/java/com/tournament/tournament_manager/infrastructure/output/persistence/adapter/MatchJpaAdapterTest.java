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
        Tournament tournament = new Tournament();
        tournament.setId(10L);
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);

        Match match = new Match();
        match.setTournament(tournament);
        match.setPlayer1(player1);
        match.setPlayer2(player2);

        when(tournamentRepository.getReferenceById(10L)).thenReturn(new TournamentEntity());
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
        Tournament tournament = new Tournament();
        tournament.setId(10L);
        Player player1 = new Player();
        player1.setId(1L);

        Match match = new Match();
        match.setTournament(tournament);
        match.setPlayer1(player1);
        match.setPlayer2(null);

        when(tournamentRepository.getReferenceById(10L)).thenReturn(new TournamentEntity());
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
