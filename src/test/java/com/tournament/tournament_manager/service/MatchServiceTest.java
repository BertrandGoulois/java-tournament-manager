package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

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

        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }
}