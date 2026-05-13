package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private MatchService matchService;

    @Test
    void recordMatchResult_shouldThrow_whenMatchNotFound() {
        when(matchRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(MatchNotFoundException.class,
                () -> matchService.recordMatchResult(1L, new RecordMatchResultRequest(1L)));
    }

    @Test
    void recordMatchResult_shouldThrow_whenMatchAlreadyFinished() {
        Match match = new Match();
        match.setStatus(MatchStatus.FINISHED);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
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

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
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

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any())).thenReturn(match);

        matchService.recordMatchResult(1L, new RecordMatchResultRequest(1L));

        verify(kafkaTemplate, times(1)).send(anyString(), any());
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

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        MatchResponse response = matchService.getMatchById(1L);

        assertEquals(1L, response.id());
    }

    @Test
    void getMatchById_shouldThrow_whenNotFound() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());
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

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any())).thenReturn(match);

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

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        MatchResponse response = matchService.getMatchById(1L);

        assertNull(response.player2Id());
        assertNull(response.winnerId());
    }
}