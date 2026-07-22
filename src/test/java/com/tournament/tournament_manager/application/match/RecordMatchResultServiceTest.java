package com.tournament.tournament_manager.application.match;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.match.PublishMatchEventPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
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
                () -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(1L)));
    }

    @Test
    void recordMatchResult_shouldThrow_whenMatchAlreadyFinished() {
        Match match = new Match();
        match.setStatus(MatchStatus.FINISHED);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        assertThrows(InvalidException.class,
                () -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(1L)));
    }

    @Test
    void recordMatchResult_shouldThrow_whenWinnerNotInMatch() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = new Match();
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        assertThrows(InvalidException.class,
                () -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(99L)));
    }

    @Test
    void recordMatchResult_shouldPublishEvent_whenValid() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = new Match();
        match.setTournament(new Tournament());
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);
        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(1L));
        verify(publishMatchEventPort, times(1)).publishMatchFinished(any());
    }

    @Test
    void recordMatchResult_shouldSetPlayer2AsWinner_whenWinnerIsPlayer2() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = new Match();
        match.setTournament(new Tournament());
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);
        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(2L));
        assertEquals(player2, match.getWinner());
    }

    @Test
    void recordMatchResult_shouldThrow_whenWinnerIsNullPlayer2Bye() {
        Player player1 = new Player();
        player1.setId(1L);
        Match match = new Match();
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(null); // bye
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        assertThrows(InvalidException.class,
                () -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(99L)));
    }

    @Test
    void recordMatchResult_shouldSetMatchStatusToFinished() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = new Match();
        match.setTournament(new Tournament());
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(1L));

        assertEquals(MatchStatus.FINISHED, match.getStatus());
    }

    @Test
    void recordMatchResult_shouldSetPlayer1AsWinner_whenWinnerIsPlayer1() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Match match = new Match();
        match.setTournament(new Tournament());
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(1L));

        assertEquals(player1, match.getWinner());
    }

    @Test
    void recordMatchResult_shouldReturnResponse_withCorrectFields() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Tournament tournament = new Tournament();
        tournament.setId(10L);
        Match match = new Match();
        match.setId(5L);
        match.setTournament(tournament);
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        when(loadMatchPort.loadMatch(5L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        var response = recordMatchResultService.recordMatchResult(5L, new RecordMatchResultRequest(1L));

        assertEquals(5L, response.id());
        assertEquals(10L, response.tournamentId());
        assertEquals(1L, response.player1Id());
        assertEquals(2L, response.player2Id());
        assertEquals(MatchStatus.FINISHED, response.status());
    }

    @Test
    void recordMatchResult_shouldReturnNullPlayer2Id_whenPlayer2IsNull() {
        Player player1 = new Player();
        player1.setId(1L);
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Match match = new Match();
        match.setId(1L);
        match.setTournament(tournament);
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(null);
        match.setWinner(player1);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        var response = recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(1L));

        assertEquals(null, response.player2Id());
        assertEquals(1L, response.winnerId());
    }

    @Test
    void recordMatchResult_shouldNotThrow_whenPlayer2NullAndWinnerIsPlayer1() {
        Player player1 = new Player();
        player1.setId(1L);
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Match match = new Match();
        match.setId(1L);
        match.setTournament(tournament);
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        assertDoesNotThrow(() -> recordMatchResultService.recordMatchResult(1L, new RecordMatchResultRequest(1L)));
    }
}