package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.service.BracketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BracketListenerTest {

    @Mock
    private BracketService bracketService;
    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private BracketListener bracketListener;

    @Test
    void onMatchFinished_shouldCallAdvanceToNextRound() {
        Tournament tournament = new Tournament();
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(4);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        bracketListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(bracketService, times(1)).advanceToNextRound(tournament, 4);
    }

    @Test
    void onMatchFinished_shouldThrow_whenMatchNotFound() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MatchNotFoundException.class,
                () -> bracketListener.onMatchFinished(new MatchFinishedEvent(99L)));
    }
}