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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}