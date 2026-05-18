package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.in.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.out.LoadMatchPort;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BracketListenerTest {

    @Mock
    private AdvanceBracketUseCase advanceBracketUseCase;
    @Mock
    private LoadMatchPort loadMatchPort;

    @InjectMocks
    private BracketListener bracketListener;

    @Test
    void onMatchFinished_shouldCallAdvanceToNextRound() {
        Tournament tournament = new Tournament();
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(4);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        bracketListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(advanceBracketUseCase, times(1)).advanceToNextRound(tournament, 4);
    }

    @Test
    void onMatchFinished_shouldThrow_whenMatchNotFound() {
        when(loadMatchPort.loadMatch(99L)).thenThrow(new MatchNotFoundException(99L));

        assertThrows(MatchNotFoundException.class,
                () -> bracketListener.onMatchFinished(new MatchFinishedEvent(99L)));
    }
}