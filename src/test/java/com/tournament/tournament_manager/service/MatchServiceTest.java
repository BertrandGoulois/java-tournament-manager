package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.match.PublishMatchEventPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private LoadMatchPort loadMatchPort;
    @Mock
    private SaveMatchPort saveMatchPort;
    @Mock
    private PublishMatchEventPort publishMatchEventPort;

    @InjectMocks
    private MatchService matchService;

    @Test
    void recordMatchResult_shouldThrow_whenMatchNotFound() {
        when(loadMatchPort.loadMatch(1L)).thenThrow(new MatchNotFoundException(1L));
        assertThrows(MatchNotFoundException.class,
                () -> matchService.recordMatchResult(1L, new RecordMatchResultRequest(1L)));
    }

    @Test
    void recordMatchResult_shouldThrow_whenMatchAlreadyFinished() {
        Match match = new Match();
        match.setStatus(MatchStatus.FINISHED);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        assertThrows(InvalidException.class,
                () -> matchService.recordMatchResult(1L, new RecordMatchResultRequest(1L)));
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
                () -> matchService.recordMatchResult(1L, new RecordMatchResultRequest(99L)));
    }

    @Test
    void recordMatchResult_shouldPublishEvent_whenValid() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);

        Match match = new Match();
        Tournament tournament = new Tournament();
        match.setTournament(tournament);
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        matchService.recordMatchResult(1L, new RecordMatchResultRequest(1L));

        verify(publishMatchEventPort, times(1)).publishMatchFinished(any());
    }

    @Test
    void getMatchById_shouldReturnMatch_whenFound() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);

        Tournament tournament = new Tournament();
        tournament.setId(1L);

        Match match = new Match();
        match.setId(1L);
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setTournament(tournament);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        MatchResponse response = matchService.getMatchById(1L);

        assertEquals(1L, response.id());
    }

    @Test
    void getMatchById_shouldThrow_whenNotFound() {
        when(loadMatchPort.loadMatch(99L)).thenThrow(new MatchNotFoundException(99L));
        assertThrows(MatchNotFoundException.class, () -> matchService.getMatchById(99L));
    }

    @Test
    void recordMatchResult_shouldSetPlayer2AsWinner_whenWinnerIsPlayer2() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);

        Tournament tournament = new Tournament();
        Match match = new Match();
        match.setTournament(tournament);
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(saveMatchPort.saveMatch(any())).thenReturn(match);

        matchService.recordMatchResult(1L, new RecordMatchResultRequest(2L));

        assertEquals(player2, match.getWinner());
    }

    @Test
    void getMatchById_shouldReturnMatch_withNullPlayer2AndWinner() {
        Player player1 = new Player();
        player1.setId(1L);

        Tournament tournament = new Tournament();
        tournament.setId(1L);

        Match match = new Match();
        match.setId(1L);
        match.setStatus(MatchStatus.FINISHED);
        match.setPlayer1(player1);
        match.setPlayer2(null);
        match.setWinner(null);
        match.setTournament(tournament);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        MatchResponse response = matchService.getMatchById(1L);

        assertNull(response.player2Id());
        assertNull(response.winnerId());
    }
}