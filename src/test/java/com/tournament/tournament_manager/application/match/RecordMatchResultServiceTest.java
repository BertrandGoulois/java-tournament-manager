package com.tournament.tournament_manager.application.match;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.match.PublishMatchEventPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.model.RecordMatchResultCommand;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordMatchResultServiceTest {

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();
    @Mock
    private LoadMatchPort loadMatchPort;
    @Mock
    private SaveMatchPort saveMatchPort;
    @Mock
    private PublishMatchEventPort publishMatchEventPort;

    private RecordMatchResultService recordMatchResultService;

    @BeforeEach
    void setUp() {
        recordMatchResultService = new RecordMatchResultService(loadMatchPort, saveMatchPort, publishMatchEventPort, meterRegistry);
    }

    @Test
    void recordMatchResult_shouldThrow_whenMatchNotFound() {
        when(loadMatchPort.loadMatch(1L)).thenThrow(new MatchNotFoundException(1L));
        assertThrows(MatchNotFoundException.class,
                () -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L)));
    }

    @Test
    void recordMatchResult_shouldThrow_whenMatchAlreadyFinished() {
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.FINISHED, null, null, null, null, null, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        assertThrows(InvalidException.class,
                () -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L)));
    }

    @Test
    void recordMatchResult_shouldThrow_whenWinnerNotInMatch() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, player1, player2, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        assertThrows(InvalidException.class,
                () -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(99L)));
    }

    @Test
    void recordMatchResult_shouldPublishEvent_whenValid() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, Tournament.reconstitute(null, new TournamentName("Test"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null), player1, player2, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);
        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L));
        verify(publishMatchEventPort, times(1)).publishMatchFinished(any(), any());
    }

    @Test
    void recordMatchResult_shouldCarryPreMatchEloInEvent() {
        // L'événement doit porter les ELO d'AVANT match, capturés au moment de la
        // publication (synchrone), pour que CommentaryListener n'ait jamais besoin de
        // relire un ELO potentiellement déjà modifié par EloListener (consumer group
        // indépendant, asynchrone).
        Player player1 = new Player();
        player1.setId(1L);
        player1.setEloRating(new com.tournament.tournament_manager.domain.model.valueobjects.EloRating(1200));
        Player player2 = new Player();
        player2.setId(2L);
        player2.setEloRating(new com.tournament.tournament_manager.domain.model.valueobjects.EloRating(1000));
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, Tournament.reconstitute(null, new TournamentName("Test"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null), player1, player2, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L));

        org.mockito.ArgumentCaptor<com.tournament.tournament_manager.domain.event.MatchFinishedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(com.tournament.tournament_manager.domain.event.MatchFinishedEvent.class);
        verify(publishMatchEventPort).publishMatchFinished(captor.capture(), any());

        assertEquals(1200, captor.getValue().player1EloBefore());
        assertEquals(1000, captor.getValue().player2EloBefore());
    }

    @Test
    void recordMatchResult_shouldSetPlayer2AsWinner_whenWinnerIsPlayer2() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, Tournament.reconstitute(null, new TournamentName("Test"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null), player1, player2, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);
        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(2L));
        assertEquals(player2, match.getWinner());
    }

    @Test
    void recordMatchResult_shouldThrow_whenWinnerIsNullPlayer2Bye() {
        Player player1 = new Player();
        player1.setId(1L);
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, player1, null, null); // bye : player2 déjà null
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        assertThrows(InvalidException.class,
                () -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(99L)));
    }

    @Test
    void recordMatchResult_shouldSetMatchStatusToFinished() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, Tournament.reconstitute(null, new TournamentName("Test"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null), player1, player2, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L));

        assertEquals(MatchStatus.FINISHED, match.getStatus());
    }

    @Test
    void recordMatchResult_shouldSetPlayedAt_whenValid() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, Tournament.reconstitute(null, new TournamentName("Test"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null), player1, player2, null);
        assertNull(match.getPlayedAt());
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        java.time.Instant before = java.time.Instant.now();
        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L));
        java.time.Instant after = java.time.Instant.now();

        assertNotNull(match.getPlayedAt());
        assertFalse(match.getPlayedAt().isBefore(before));
        assertFalse(match.getPlayedAt().isAfter(after));
    }

    @Test
    void recordMatchResult_shouldSetPlayer1AsWinner_whenWinnerIsPlayer1() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, Tournament.reconstitute(null, new TournamentName("Test"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null), player1, player2, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L));

        assertEquals(player1, match.getWinner());
    }

    @Test
    void recordMatchResult_shouldReturnResponse_withCorrectFields() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Tournament tournament = Tournament.reconstitute(10L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(5L, 0, 0, null, MatchStatus.PENDING, null, null, tournament, player1, player2, null);
        when(loadMatchPort.loadMatch(5L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        var result = recordMatchResultService.recordMatchResult(5L, new RecordMatchResultCommand(1L));

        assertEquals(5L, result.getId());
        assertEquals(10L, result.getTournament().getId());
        assertEquals(1L, result.getPlayer1().getId());
        assertEquals(2L, result.getPlayer2().getId());
        assertEquals(MatchStatus.FINISHED, result.getStatus());
    }

    @Test
    void recordMatchResult_shouldReturnNullPlayer2Id_whenPlayer2IsNull() {
        Player player1 = new Player();
        player1.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.PENDING, null, null, tournament, player1, null, player1);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        var result = recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L));

        assertEquals(null, result.getPlayer2());
        assertEquals(1L, result.getWinner().getId());
    }

    @Test
    void recordMatchResult_shouldNotThrow_whenPlayer2NullAndWinnerIsPlayer1() {
        Player player1 = new Player();
        player1.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.PENDING, null, null, tournament, player1, null, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        assertDoesNotThrow(() -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultCommand(1L)));
    }
}